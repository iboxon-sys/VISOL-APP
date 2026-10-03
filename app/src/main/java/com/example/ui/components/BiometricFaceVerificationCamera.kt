package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Liveness Verification States (Fintech / Banking Security Standard):
 * ALIGN_FACE: Yuzni dumaloq ramka ichiga keltirish
 * STEP_A_BLINK: Ko'zni bir marta yuming va oching
 * STEP_B_TIMER: 10 soniyalik kutilma (erkin harakatlanish)
 * CAPTURING: Async 1:1 kvadrat surat olish va saqlash
 * SUCCESS: Muvaffaqiyatli tasdiqlandi
 */
enum class LivenessChallenge {
    GENTLE_SMILE,
    TURN_HEAD
}

enum class VerificationState {
    ALIGN_FACE,      // Yuzni aylanaga to'g'rilash
    STEP_A_BLINK,    // 1-qadam: Ko'zni pirpiratish
    READY_TO_SNAP,   // Jonlilik tekshirildi! Suratga olishga tayyor
    CAPTURING,       // Surat olinmoqda
    SUCCESS          // Surat olindi va tasdiqlandi
}

/**
 * Circle Mask Bounding Box Validator (Dumaloq ramka nazorati)
 * Checks continuously if the face bounding box stays inside the circular mask boundary.
 */
object CircleBoundingBoxValidator {
    fun isFaceInCircleBounds(
        face: Face,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int
    ): Boolean {
        val frameWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageHeight.toFloat() else imageWidth.toFloat()
        val frameHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageWidth.toFloat() else imageHeight.toFloat()

        if (frameWidth <= 0f || frameHeight <= 0f) return false

        val box = face.boundingBox
        val faceCenterX = box.centerX().toFloat()
        val faceCenterY = box.centerY().toFloat()
        val faceWidth = box.width().toFloat()
        val faceHeight = box.height().toFloat()

        val maskCenterX = frameWidth / 2f
        val maskCenterY = frameHeight / 2f
        val circleRadius = minOf(frameWidth, frameHeight) * 0.40f

        val faceMaxRadius = maxOf(faceWidth, faceHeight) / 2f

        // 1. Circle radius must accommodate face: circleRadius >= max(face.width, face.height) / 2
        if (circleRadius < faceMaxRadius) {
            return false // Head is too large / exceeds circle boundary
        }

        // 2. face.centerX and face.centerY must remain strictly inside overlay circle radius
        val dx = kotlin.math.abs(faceCenterX - maskCenterX)
        val dy = kotlin.math.abs(faceCenterY - maskCenterY)
        val centerDistance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (centerDistance > circleRadius * 0.60f) {
            return false // Face center is outside overlay circle bounds
        }

        return true
    }
}

@Composable
fun BiometricFaceVerificationCamera(
    onVerificationSuccess: (Bitmap) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var verificationState by remember { mutableStateOf(VerificationState.ALIGN_FACE) }
    var statusPrompt by remember { mutableStateOf("Yuzingizni aylanaga to'g'rilang") }
    var dynamicProgress by remember { mutableFloatStateOf(0.1f) }
    var isFaceDetectedInMask by remember { mutableStateOf(false) }
    var isCapturingInProgress by remember { mutableStateOf(false) }
    var capturedBitmapFromFrame by remember { mutableStateOf<Bitmap?>(null) }
    var savedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    var timerSeconds by remember { mutableIntStateOf(10) }
    var blinkStepPhase by remember { mutableIntStateOf(0) }

    // CameraX components
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // ML Kit Face Detector
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setMinFaceSize(0.25f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            faceDetector.close()
        }
    }

    // Image Capture triggered by manual button click
    LaunchedEffect(verificationState) {
        if (verificationState == VerificationState.CAPTURING && !isCapturingInProgress) {
            isCapturingInProgress = true
            statusPrompt = "Surat saqlanmoqda..."

            // 1. First try instant live preview bitmap
            val livePreviewBmp = previewViewRef?.bitmap
            var finalBitmap: Bitmap? = null
            if (livePreviewBmp != null) {
                try {
                    val dimension = minOf(livePreviewBmp.width, livePreviewBmp.height)
                    val x = (livePreviewBmp.width - dimension) / 2
                    val y = (livePreviewBmp.height - dimension) / 2
                    finalBitmap = Bitmap.createBitmap(livePreviewBmp, x, y, dimension, dimension)
                } catch (e: Exception) {
                    Log.e("BiometricCamera", "Crop error: ${e.message}")
                }
            }

            // 2. If not available, use captured frame bitmap
            if (finalBitmap == null) {
                finalBitmap = capturedBitmapFromFrame
            }

            // 3. Fallback to imageCapture.takePicture
            if (finalBitmap == null) {
                finalBitmap = withTimeoutOrNull(4000L) {
                    suspendCancellableCoroutine<Bitmap?> { continuation ->
                        captureAndCropSquareAsync(
                            imageCapture = imageCapture,
                            context = context,
                            onSuccess = { bitmap ->
                                if (continuation.isActive) continuation.resume(bitmap)
                            },
                            onError = { exc ->
                                Log.e("BiometricCamera", "Suratga olishda xatolik: ${exc.message}")
                                if (continuation.isActive) continuation.resume(null)
                            }
                        )
                    }
                }
            }

            isCapturingInProgress = false
            if (finalBitmap != null) {
                verificationState = VerificationState.SUCCESS
                statusPrompt = "Surat tayyor! Tasdiqlang yoki qayta oling ✓"
                dynamicProgress = 1.0f
                savedBitmap = finalBitmap
            } else {
                verificationState = VerificationState.READY_TO_SNAP
                statusPrompt = "Qaytadan suratga oling 📸"
                Toast.makeText(context, "Suratga olishda xatolik, qayta urinib ko'ring", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dynamic progress color
    val animatedProgress by animateFloatAsState(
        targetValue = dynamicProgress,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "biometric_progress"
    )

    val progressColor by animateColorAsState(
        targetValue = when (verificationState) {
            VerificationState.SUCCESS -> Color(0xFF10B981) // Green
            VerificationState.CAPTURING -> Color(0xFF06B6D4) // Cyan
            VerificationState.READY_TO_SNAP -> Color(0xFF10B981) // Green Ready
            VerificationState.STEP_A_BLINK -> Color(0xFFF59E0B) // Amber
            else -> Color(0xFF6366F1) // Indigo
        },
        label = "biometric_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Face ID Liveness Check",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Fintech Biometrik Xavfsizlik Standarti",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Bekor qilish",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1:1 Square Camera Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        previewViewRef = previewView

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                                .build()
                                .also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                                .build()

                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        analyzeFaceFrame(
                                            imageProxy = imageProxy,
                                            faceDetector = faceDetector,
                                            getVerificationState = { verificationState },
                                            getBlinkStepPhase = { blinkStepPhase },
                                            onFaceCaptured = { bitmap ->
                                                capturedBitmapFromFrame = bitmap
                                            },
                                            onStateUpdate = { newState, newPrompt, progress, inMask, newBlink ->
                                                verificationState = newState
                                                statusPrompt = newPrompt
                                                dynamicProgress = progress
                                                isFaceDetectedInMask = inMask
                                                blinkStepPhase = newBlink
                                            }
                                        )
                                    }
                                }

                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture,
                                    imageAnalyzer
                                )
                            } catch (exc: Exception) {
                                Log.e("BiometricCamera", "Kamera bog'lashda xatolik", exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Circular Overlay Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val radius = (minOf(canvasWidth, canvasHeight) * 0.40f)
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

                    drawRect(
                        color = Color.Black.copy(alpha = 0.52f),
                        size = Size(canvasWidth, canvasHeight)
                    )

                    drawCircle(
                        color = Color.Transparent,
                        radius = radius,
                        center = center,
                        blendMode = androidx.compose.ui.graphics.BlendMode.Clear
                    )

                    drawCircle(
                        color = if (isFaceDetectedInMask) Color.White.copy(alpha = 0.6f) else Color.Red.copy(alpha = 0.6f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    val sweepAngle = animatedProgress * 360f
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                if (!isFaceDetectedInMask) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Status Prompt Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = progressColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, progressColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = progressColor,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (verificationState) {
                                    VerificationState.SUCCESS -> Icons.Default.Check
                                    VerificationState.STEP_A_BLINK -> Icons.Default.Visibility
                                    VerificationState.READY_TO_SNAP -> Icons.Default.CameraAlt
                                    else -> Icons.Default.Person
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusPrompt,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (verificationState) {
                                VerificationState.ALIGN_FACE -> "Yuzingizni dumaloq ramka ichida tuting"
                                VerificationState.STEP_A_BLINK -> "1-qadam: Ko'zingizni bir marta yuming va oching"
                                VerificationState.READY_TO_SNAP -> "2-qadam: Chiroyli holatda 'Suratga olish' tugmasini bosing"
                                VerificationState.CAPTURING -> "Surat olinmoqda..."
                                VerificationState.SUCCESS -> "Surat tayyor! Tasdiqlang yoki qayta oling"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Steps Tracker Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LivenessStepChip(
                    title = "1. Yuzni to'g'rilash",
                    isDone = verificationState != VerificationState.ALIGN_FACE
                )
                LivenessStepChip(
                    title = "2. Pirpiratish",
                    isDone = verificationState == VerificationState.READY_TO_SNAP || verificationState == VerificationState.CAPTURING || verificationState == VerificationState.SUCCESS
                )
                LivenessStepChip(
                    title = "3. Suratga olish",
                    isDone = verificationState == VerificationState.SUCCESS
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
            val velvetPrimary = Color(0xFFE11D48)

            if (savedBitmap == null) {
                // Shutter Button (Enabled only when liveness has passed!)
                val isReadyToSnap = verificationState == VerificationState.READY_TO_SNAP
                Button(
                    onClick = {
                        val livePreviewBmp = previewViewRef?.bitmap ?: capturedBitmapFromFrame
                        if (livePreviewBmp != null) {
                            try {
                                val dimension = minOf(livePreviewBmp.width, livePreviewBmp.height)
                                val x = (livePreviewBmp.width - dimension) / 2
                                val y = (livePreviewBmp.height - dimension) / 2
                                val squareBmp = Bitmap.createBitmap(livePreviewBmp, x, y, dimension, dimension)
                                savedBitmap = squareBmp
                                verificationState = VerificationState.SUCCESS
                                statusPrompt = "Surat tayyor! Tasdiqlang yoki qayta oling ✓"
                                dynamicProgress = 1.0f
                            } catch (e: Exception) {
                                Log.e("BiometricCamera", "Kvadrat qirqishda xatolik", e)
                                verificationState = VerificationState.CAPTURING
                            }
                        } else {
                            verificationState = VerificationState.CAPTURING
                        }
                    },
                    enabled = isReadyToSnap,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isReadyToSnap) Color(0xFF10B981) else velvetPrimary,
                        disabledContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        contentColor = Color.White,
                        disabledContentColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isReadyToSnap) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📸 Tayyor / Suratga olish", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    } else {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jonlilik tekshirilmoqda...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                // Preview Actions: Confirm or Retake
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            savedBitmap?.let {
                                onVerificationSuccess(it)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✓ Ushbu suratni saqlash", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            savedBitmap = null
                            verificationState = VerificationState.READY_TO_SNAP
                            statusPrompt = "Qaytadan suratga oling 📸"
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = velvetPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🔄 Qaytadan rasmga olish", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = velvetPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LivenessStepChip(
    title: String,
    isDone: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDone) Color(0xFFE8F5E9) else Color(0xFFF1F5F9),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isDone) Color(0xFF2E7D32) else Color(0xFF94A3B8),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
                color = if (isDone) Color(0xFF1B5E20) else Color(0xFF64748B)
            )
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun analyzeFaceFrame(
    imageProxy: ImageProxy,
    faceDetector: com.google.mlkit.vision.face.FaceDetector,
    getVerificationState: () -> VerificationState,
    getBlinkStepPhase: () -> Int,
    onFaceCaptured: (Bitmap) -> Unit,
    onStateUpdate: (
        newState: VerificationState,
        newPrompt: String,
        progress: Float,
        inMask: Boolean,
        newBlink: Int
    ) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    faceDetector.process(image)
        .addOnSuccessListener { faces ->
            val currentState = getVerificationState()
            val currentBlink = getBlinkStepPhase()

            if (currentState == VerificationState.CAPTURING || currentState == VerificationState.SUCCESS) {
                return@addOnSuccessListener
            }

            if (faces.size != 1) {
                val prompt = if (faces.isEmpty()) "Yuzingizni aylanaga to'g'rilang" else "Kadrda faqat bitta yuz bo'lishi shart!"
                onStateUpdate(
                    VerificationState.ALIGN_FACE,
                    prompt,
                    0.1f,
                    false,
                    0
                )
                return@addOnSuccessListener
            }

            val face = faces[0]

            val isInCircle = CircleBoundingBoxValidator.isFaceInCircleBounds(
                face = face,
                imageWidth = imageProxy.width,
                imageHeight = imageProxy.height,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees
            )

            if (!isInCircle) {
                onStateUpdate(
                    VerificationState.ALIGN_FACE,
                    "Bashingizni dumaloq ramka ichida tuting",
                    0.15f,
                    false,
                    currentBlink
                )
                return@addOnSuccessListener
            }

            if (isInCircle) {
                try {
                    val rawBitmap = imageProxy.toBitmap()
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val matrix = Matrix().apply {
                        postRotate(rotation.toFloat())
                        postScale(-1f, 1f) // Selfie mirror effect
                    }
                    val rotatedBitmap = Bitmap.createBitmap(
                        rawBitmap,
                        0,
                        0,
                        rawBitmap.width,
                        rawBitmap.height,
                        matrix,
                        true
                    )
                    val dimension = minOf(rotatedBitmap.width, rotatedBitmap.height)
                    val x = (rotatedBitmap.width - dimension) / 2
                    val y = (rotatedBitmap.height - dimension) / 2
                    val squareBitmap = Bitmap.createBitmap(
                        rotatedBitmap,
                        x,
                        y,
                        dimension,
                        dimension
                    )
                    onFaceCaptured(squareBitmap)
                } catch (e: Exception) {
                    // Frame snapshot error ignored
                }
            }

            val leftEye = face.leftEyeOpenProbability ?: -1f
            val rightEye = face.rightEyeOpenProbability ?: -1f

            when (currentState) {
                VerificationState.ALIGN_FACE -> {
                    onStateUpdate(
                        VerificationState.STEP_A_BLINK,
                        "Ko'zingizni yuming va oching",
                        0.30f,
                        true,
                        0
                    )
                }

                VerificationState.STEP_A_BLINK -> {
                    var currentBlinkVal = currentBlink
                    if (currentBlinkVal == 0) {
                        if (leftEye > 0.50f && rightEye > 0.50f) {
                            currentBlinkVal = 1
                        }
                    } else if (currentBlinkVal == 1) {
                        // Eye closed condition (< 0.20f)
                        if (leftEye < 0.20f && rightEye < 0.20f && leftEye >= 0f && rightEye >= 0f) {
                            currentBlinkVal = 2
                        }
                    } else if (currentBlinkVal == 2) {
                        // Eye open condition (> 0.80f) -> Liveness Passed!
                        if (leftEye > 0.80f && rightEye > 0.80f) {
                            onStateUpdate(
                                VerificationState.READY_TO_SNAP,
                                "Jonlilik tasdiqlandi! Suratga olish tugmasini bosing 📸",
                                1.0f,
                                true,
                                3
                            )
                            return@addOnSuccessListener
                        }
                    }

                    onStateUpdate(
                        VerificationState.STEP_A_BLINK,
                        "Ko'zingizni bir marta yuming va oching",
                        0.35f + (currentBlinkVal * 0.15f),
                        true,
                        currentBlinkVal
                    )
                }

                else -> {}
            }
        }
        .addOnFailureListener {
            Log.e("BiometricCamera", "Face detector xatoligi", it)
        }
        .addOnCompleteListener {
            imageProxy.close()
        }
}

/**
 * Async 1:1 Square Capture & Crop on Dispatchers.IO
 */
private fun captureAndCropSquareAsync(
    imageCapture: ImageCapture?,
    context: Context,
    onSuccess: (Bitmap) -> Unit,
    onError: (Exception) -> Unit
) {
    if (imageCapture == null) {
        onError(IllegalStateException("ImageCapture ishga tushmagan"))
        return
    }

    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val rawBitmap = imageProxy.toBitmap()
                        val rotation = imageProxy.imageInfo.rotationDegrees

                        val matrix = Matrix().apply {
                            postRotate(rotation.toFloat())
                            postScale(-1f, 1f) // Selfie mirror effect
                        }
                        val rotatedBitmap = Bitmap.createBitmap(
                            rawBitmap,
                            0,
                            0,
                            rawBitmap.width,
                            rawBitmap.height,
                            matrix,
                            true
                        )

                        val dimension = minOf(rotatedBitmap.width, rotatedBitmap.height)
                        val x = (rotatedBitmap.width - dimension) / 2
                        val y = (rotatedBitmap.height - dimension) / 2

                        val squareBitmap = Bitmap.createBitmap(
                            rotatedBitmap,
                            x,
                            y,
                            dimension,
                            dimension
                        )

                        imageProxy.close()
                        withContext(Dispatchers.Main) {
                            onSuccess(squareBitmap)
                        }
                    } catch (e: Exception) {
                        imageProxy.close()
                        withContext(Dispatchers.Main) {
                            onError(e)
                        }
                    }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}
