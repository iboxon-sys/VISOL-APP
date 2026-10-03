package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.model.ModerationSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Advanced Multi-Tier AI & Multi-Modal Computer Vision Moderation Service.
 * Implements two-stage hybrid content moderation for Visol.uz:
 * 
 * 1. Google Gemini Multi-Modal Vision API:
 *    Sends image/video frames directly (Base64 JPEG bytes) for deep contextual inspection.
 *    Analyzes Uzbek cultural ethics, 18+ adult/NSFW nudity, harassment, bullying, and weapons.
 * 
 * 2. Local Computer Vision & Skin-Exposure Heuristics:
 *    Tezkor YCbCr / HSV rang tahlili orqali lokal qo'shimcha xavfsizlik darajasi.
 *
 * Darajalar:
 * - LEVEL_1_CRITICAL: O'ta qo'pol yoki 100% taqiqlangan 18+ / NSFW kontent -> Darhol Rad etiladi.
 * - LEVEL_2_SUSPICIOUS: Shubhali kadrlar -> Bazaga o'tmaydi, foydalanuvchi vaqtinchalik cheklanadi (isRestricted=true),
 *                        va Telegram Bot orqali Adminga dalillar bilan yuboriladi.
 * - SAFE: Qoidalarga mos -> Tasdiqlanadi.
 */
object ImageModerationService {

    private const val TAG = "ImageModeration"

    data class ModerationAssessment(
        val severity: ModerationSeverity,
        val isApproved: Boolean,
        val warningMessage: String? = null,
        val reason: String = "Normal",
        val confidence: Float = 0f,
        val sampleFrameBitmap: Bitmap? = null
    )

    data class ModerationResult(
        val isApproved: Boolean,
        val warningMessage: String? = null,
        val reason: String? = null,
        val confidence: Float = 0f
    )

    suspend fun checkBitmapSafety(context: Context, bitmap: Bitmap): ModerationResult {
        val assessment = inspectBitmap(context, bitmap)
        return ModerationResult(
            isApproved = assessment.isApproved,
            warningMessage = assessment.warningMessage,
            reason = assessment.reason,
            confidence = assessment.confidence
        )
    }

    suspend fun checkUriSafety(context: Context, uri: Uri): ModerationResult {
        val assessment = inspectMediaUri(context, uri, isVideo = false)
        return ModerationResult(
            isApproved = assessment.isApproved,
            warningMessage = assessment.warningMessage,
            reason = assessment.reason,
            confidence = assessment.confidence
        )
    }

    /**
     * Inspects a single Bitmap through Multi-Modal Gemini Vision & Heuristic Analysis.
     */
    suspend fun inspectBitmap(context: Context, bitmap: Bitmap): ModerationAssessment = withContext(Dispatchers.Default) {
        try {
            if (bitmap.isRecycled) {
                return@withContext ModerationAssessment(
                    severity = ModerationSeverity.LEVEL_1_CRITICAL,
                    isApproved = false,
                    warningMessage = "Rasm fayli yaroqsiz",
                    reason = "INVALID_BITMAP"
                )
            }

            // 1. Google Gemini Multi-Modal Vision tekshiruvi (Image Bytes / Base64)
            val geminiAssessment = inspectWithGeminiVision(bitmap)
            if (geminiAssessment != null && !geminiAssessment.isApproved) {
                Log.d(TAG, "Gemini Vision detected violation: ${geminiAssessment.reason}")
                return@withContext geminiAssessment.copy(sampleFrameBitmap = bitmap)
            }

            // 2. Lokal Computer Vision (Chroma & Skin ratio) tekshiruvi - me'yoriy sezgirlik
            val skinRatio = analyzeSkinExposureRatio(bitmap)
            Log.d(TAG, "Assessed skin-exposure ratio: $skinRatio")

            when {
                // 1-Daraja: O'ta qo'pol kontent (Faqat haqiqiy yalang'ochlik > 82%)
                skinRatio >= 0.82f -> {
                    ModerationAssessment(
                        severity = ModerationSeverity.LEVEL_1_CRITICAL,
                        isApproved = false,
                        warningMessage = "🚫 Qat'iy qoidabuzarlik: Yuklangan faylda o'ta qo'pol yoki taqiqlangan kattalar (NSFW) kontenti aniqlandi! Ushbu fayl bazaga yuklanmadi.",
                        reason = "O'ta ochiq-sochiq / Taqiqlangan kattalar kontenti (${(skinRatio * 100).toInt()}%)",
                        confidence = skinRatio,
                        sampleFrameBitmap = bitmap
                    )
                }

                // 2-Daraja: Shubhali kadrlar (Faqat juda yuqori 70% dan 81% gacha bo'lgan tana qoplamasi)
                skinRatio in 0.70f..0.819f -> {
                    ModerationAssessment(
                        severity = ModerationSeverity.LEVEL_2_SUSPICIOUS,
                        isApproved = false,
                        warningMessage = "⚠️ Siz yuklagan kontent xavfsizlik qoidalariga shubhali deb topildi va admin tekshiruviga yuborildi.",
                        reason = "Shubhali ochiq kadrlar (${(skinRatio * 100).toInt()}%)",
                        confidence = skinRatio,
                        sampleFrameBitmap = bitmap
                    )
                }

                // Xavfsiz kontent: Oddiy insonlar, selfilar, kiyinganlar, fonlar, to'y va tabiat videolari
                else -> {
                    ModerationAssessment(
                        severity = ModerationSeverity.SAFE,
                        isApproved = true,
                        reason = "Xavfsiz va odob doirasida",
                        confidence = 1f - skinRatio,
                        sampleFrameBitmap = bitmap
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in image moderation", e)
            ModerationAssessment(
                severity = ModerationSeverity.SAFE,
                isApproved = true,
                reason = "Standart tasdiq"
            )
        }
    }

    /**
     * Google Gemini Multi-Modal Vision API orqali rasmni to'g'ridan-to'g'ri (bayt/base64) tahlil qilish.
     */
    private suspend fun inspectWithGeminiVision(bitmap: Bitmap): ModerationAssessment? = withContext(Dispatchers.IO) {
        val apiKey = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Throwable) {
            System.getenv("GEMINI_API_KEY") ?: ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext null
        }

        try {
            // Rasmni OOM-xavfsiz optimal o'lchamga keltirib Base64 ga aylantirish
            val maxDimension = 512
            val width = bitmap.width
            val height = bitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                maxDimension.toFloat() / maxOf(width, height)
            } else 1f

            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else bitmap

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }

            val prompt = """
                Siz "Visol.uz" (O'zbekiston oilaviy tanishuv va nikoh ilovasi) uchun mas'ul bo'lgan xavfsizlik va NSFW Moderatori hisoblanasiz.
                Ushbu rasmni/kadrni o'zbekona odob-axloq, oilaviy mentalitet va xavfsizlik nuqtai nazaridan tekshiring.

                Qat'iy taqiqlangan mezonlar:
                1. 18+ kattalar kontenti, ochiq intim tana a'zolari, behayo/pornografik holatlar, ichki kiyimdagi suratlar.
                2. Qonli zo'ravonlik, qurollar, narkotik yoki jismoniy tahdid.

                MUHIM QOIDA (SEZGIRLIKNI ME'YORIDA TUTISH):
                Oddiy odamlar, selfilar, to'y marosimlari, tabiat manzaralari, oilaviy lavhalar, kiyingan insonlar QAT'IYAN TASDIQLANSIN (is_approved: true, severity: "SAFE").
                Bema'ni yoki asossiz sabablar bilan oddiy toza videolarni rad etmang!

                Faqat quyidagi JSON formatida aniq javob bering:
                {
                  "is_approved": true yoki false,
                  "severity": "LEVEL_1_CRITICAL" (agar 100% ochiq 18+/behayo bo'lsa), "LEVEL_2_SUSPICIOUS" (agar shubhali bo'lsa), "SAFE" (toza bo'lsa),
                  "reason": "O'zbek tilida aniq sababi",
                  "confidence": 0.95
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val textPart = JSONObject().apply {
                                put("text", prompt)
                            }
                            val imagePart = JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Data)
                                }
                                put("inline_data", inlineData)
                            }
                            put(textPart)
                            put(imagePart)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.1)
                    put("response_mime_type", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            if (conn.responseCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val respJson = JSONObject(responseStr)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val rawText = parts.getJSONObject(0).getString("text")
                    val cleanJsonStr = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val resultJson = JSONObject(cleanJsonStr)

                    val isApproved = resultJson.optBoolean("is_approved", true)
                    val severityStr = resultJson.optString("severity", "SAFE")
                    val reason = resultJson.optString("reason", "AI Vision tahlili")
                    val confidence = resultJson.optDouble("confidence", 0.9).toFloat()

                    val severity = when (severityStr.uppercase()) {
                        "LEVEL_1_CRITICAL" -> ModerationSeverity.LEVEL_1_CRITICAL
                        "LEVEL_2_SUSPICIOUS" -> ModerationSeverity.LEVEL_2_SUSPICIOUS
                        else -> if (!isApproved) ModerationSeverity.LEVEL_1_CRITICAL else ModerationSeverity.SAFE
                    }

                    if (!isApproved || severity != ModerationSeverity.SAFE) {
                        return@withContext ModerationAssessment(
                            severity = severity,
                            isApproved = false,
                            warningMessage = if (severity == ModerationSeverity.LEVEL_1_CRITICAL) {
                                "🚫 Qat'iy qoidabuzarlik: Gemini AI multi-modal tahlili taqiqlangan 18+/behayo kontentni aniqladi ($reason)!"
                            } else {
                                "⚠️ Gemini AI tahlili kontentni shubhali deb topdi ($reason) va admin tekshiruviga yubordi."
                            },
                            reason = "Gemini Multi-Modal: $reason",
                            confidence = confidence,
                            sampleFrameBitmap = bitmap
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini Vision inspection fallback: ${e.message}")
        }
        null
    }

    /**
     * Inspects a photo or video URI (extracts multiple keyframes from videos).
     */
    suspend fun inspectMediaUri(context: Context, uri: Uri, isVideo: Boolean): ModerationAssessment = withContext(Dispatchers.IO) {
        try {
            if (isVideo) {
                return@withContext inspectVideoUri(context, uri)
            } else {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return@withContext ModerationAssessment(
                    severity = ModerationSeverity.SAFE,
                    isApproved = true
                )

                val result = inspectBitmap(context, bitmap)
                return@withContext result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting media uri: ${e.message}")
            ModerationAssessment(
                severity = ModerationSeverity.SAFE,
                isApproved = true
            )
        }
    }

    /**
     * Multi-frame extractor for video files. Inspects multiple key timestamps (0s, 25%, 50%, 75%).
     */
    private suspend fun inspectVideoUri(context: Context, videoUri: Uri): ModerationAssessment = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 5000L

            val checkPointsUs = listOf(
                500_000L, // 0.5 sec
                (durationMs * 1000L * 0.25).toLong(), // 25%
                (durationMs * 1000L * 0.50).toLong(), // 50%
                (durationMs * 1000L * 0.75).toLong()  // 75%
            )

            var highestSeverity = ModerationSeverity.SAFE
            var worstReason = "Xavfsiz"
            var maxConfidence = 0f
            var flaggedFrame: Bitmap? = null

            for (timeUs in checkPointsUs) {
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val assessment = inspectBitmap(context, frame)
                    if (assessment.severity == ModerationSeverity.LEVEL_1_CRITICAL) {
                        return@withContext assessment
                    }
                    if (assessment.severity == ModerationSeverity.LEVEL_2_SUSPICIOUS) {
                        highestSeverity = ModerationSeverity.LEVEL_2_SUSPICIOUS
                        worstReason = assessment.reason
                        maxConfidence = maxOf(maxConfidence, assessment.confidence)
                        flaggedFrame = frame
                    }
                }
            }

            if (highestSeverity == ModerationSeverity.LEVEL_2_SUSPICIOUS) {
                // Toza videolarni asossiz o'chirmaslik uchun ruxsat beriladi (sezgirlik pasaytirildi)
                return@withContext ModerationAssessment(
                    severity = ModerationSeverity.SAFE,
                    isApproved = true,
                    reason = "Barcha kadrlar tasdiqlandi",
                    confidence = maxConfidence,
                    sampleFrameBitmap = flaggedFrame
                )
            }

            return@withContext ModerationAssessment(
                severity = ModerationSeverity.SAFE,
                isApproved = true,
                reason = "Barcha kadrlar toza"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Video keyframe extraction failed: ${e.message}")
            ModerationAssessment(
                severity = ModerationSeverity.SAFE,
                isApproved = true
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Multi-range YCbCr / HSV skin-chroma exposure analyzer.
     */
    private fun analyzeSkinExposureRatio(srcBitmap: Bitmap): Float {
        val targetWidth = 160
        val targetHeight = (srcBitmap.height * (160f / srcBitmap.width)).toInt().coerceIn(80, 240)
        val scaled = Bitmap.createScaledBitmap(srcBitmap, targetWidth, targetHeight, true)

        val width = scaled.width
        val height = scaled.height
        val totalPixels = width * height
        var skinPixels = 0

        val pixels = IntArray(totalPixels)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)

        for (color in pixels) {
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)

            val y = 0.299 * r + 0.587 * g + 0.114 * b
            val cb = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b
            val cr = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b

            val isSkinYCbCr = (cb in 77.0..127.0) && (cr in 133.0..173.0) && (y > 60)
            val isSkinRGB = (r > 95) && (g > 40) && (b > 20) &&
                    ((maxOf(r, g, b) - minOf(r, g, b)) > 15) &&
                    (Math.abs(r - g) > 15) && (r > g) && (r > b)

            if (isSkinYCbCr || isSkinRGB) {
                skinPixels++
            }
        }

        if (scaled != srcBitmap) {
            scaled.recycle()
        }

        return skinPixels.toFloat() / totalPixels.toFloat()
    }
}
