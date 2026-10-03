package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.UserRepository
import com.example.model.UserProfile
import com.example.ui.theme.*

data class DeviceContact(
    val name: String,
    val rawPhone: String,
    val normalizedPhone: String
)

/**
 * Real-Device Contacts Integration (NO DEMO DATA):
 * - Direct Phonebook Reading via Android ContactsContract
 * - Runtime permission request for READ_CONTACTS
 * - E.164 phone normalization (+998...)
 * - Matching with registered Visol users & SMS/Share invite for others
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindFriendsBottomSheet(
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onCandidateClick: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val followedIds by userRepository.followedCandidateIds.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var realContacts by remember { mutableStateOf<List<DeviceContact>>(emptyList()) }
    var isLoadingContacts by remember { mutableStateOf(false) }

    fun loadRealDeviceContacts() {
        isLoadingContacts = true
        val contactsList = mutableListOf<DeviceContact>()
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext()) {
                    val name = if (nameIndex >= 0) c.getString(nameIndex) ?: "Noma'lum" else "Noma'lum"
                    val rawPhone = if (numberIndex >= 0) c.getString(numberIndex) ?: "" else ""
                    val normalized = normalizePhoneNumber(rawPhone)
                    if (normalized.isNotBlank()) {
                        contactsList.add(DeviceContact(name, rawPhone, normalized))
                    }
                }
            }
        } catch (e: Exception) {
            // Permission or cursor exception handled gracefully
        }
        realContacts = contactsList.distinctBy { it.normalizedPhone }
        isLoadingContacts = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            loadRealDeviceContacts()
            Toast.makeText(context, "Kontaktlar muvaffaqiyatli sinxronlandi ✓", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Kontaktlarni o'qish uchun ruxsat berilmadi", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            loadRealDeviceContacts()
        }
    }

    // Match device contacts with registered Visol users
    val allCandidates by userRepository.candidates.collectAsState()
    val registeredCandidates: List<UserProfile> = remember(realContacts, allCandidates) {
        if (realContacts.isEmpty()) {
            emptyList()
        } else {
            allCandidates.filter { candidate ->
                realContacts.any { contact ->
                    contact.name.contains(candidate.fullName, ignoreCase = true) ||
                    candidate.fullName.contains(contact.name, ignoreCase = true) ||
                    (candidate.phone.isNotBlank() && contact.normalizedPhone == normalizePhoneNumber(candidate.phone))
                }
            }
        }
    }

    val unregisteredContacts: List<DeviceContact> = remember(realContacts, registeredCandidates) {
        realContacts.filter { contact ->
            registeredCandidates.none { candidate ->
                contact.name.contains(candidate.fullName, ignoreCase = true) ||
                (candidate.phone.isNotBlank() && contact.normalizedPhone == normalizePhoneNumber(candidate.phone))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) DeepNavyMesh else Color.White,
        contentColor = if (isDark) Color(0xFFF8FAFC) else TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)
            )
        },
        modifier = Modifier.fillMaxHeight(0.88f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Sarlavha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x33E11D48) else VelvetRoseSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PeopleAlt,
                            contentDescription = null,
                            tint = VelvetRosePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Haqiqiy Kontaktlar (Telefon kitobi)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFF8FAFC) else TextPrimary
                        )
                        Text(
                            text = if (hasPermission) "${realContacts.size} ta kontakt topildi" else "Ruxsat kutilmoqda",
                            fontSize = 12.sp,
                            color = VelvetRosePrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Yopish",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!hasPermission) {
                // Permission Request Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, VelvetRosePrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = null,
                            tint = VelvetRosePrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Telefon kontaktlarini sinxronlash",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isDark) Color.White else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Telefoningizdagi tanishlaringizdan kimlar Visol ilovasida borligini bilish va ularni topish uchun kontaktlarga ruxsat bering.",
                            fontSize = 12.sp,
                            color = if (isDark) Color(0xFF94A3B8) else TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kontaktlarga ruxsat berish ✓", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (isLoadingContacts) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = VelvetRosePrimary)
                }
            } else if (realContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Contacts,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Qurilmada kontaktlar topilmadi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFFCBD5E1) else TextPrimary
                        )
                    }
                }
            } else {
                if (registeredCandidates.isNotEmpty()) {
                    Button(
                        onClick = {
                            val candidateIds = registeredCandidates.map { it.id }
                            userRepository.followAllCandidates(candidateIds)
                            Toast.makeText(context, "Barcha do'stlarga obuna bo'lindi! ✨", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VelvetRosePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Barchasini kuzatish (${registeredCandidates.size} ta)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    // 1-BO'LIM: Visol.uz dagi tanishlar
                    if (registeredCandidates.isNotEmpty()) {
                        item {
                            Text(
                                text = "VISOL.UZ DАGI TANISHLARINGIZ (${registeredCandidates.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF94A3B8) else TextSecondary,
                                letterSpacing = 1.sp
                            )
                        }

                        items(registeredCandidates, key = { it.id }) { candidate ->
                            val isFollowed = followedIds.contains(candidate.id)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC))
                                    .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onDismiss()
                                            onCandidateClick(candidate)
                                        }
                                ) {
                                    ProfileAvatar(
                                        photoPath = candidate.photoPath,
                                        gender = candidate.gender,
                                        size = 44.dp,
                                        showCameraBadge = false
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = candidate.fullName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFF8FAFC) else TextPrimary
                                        )
                                        Text(
                                            text = "${candidate.age} yosh • ${candidate.viloyat}",
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else TextSecondary
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        val nowFollowing = userRepository.toggleFollowCandidate(candidate.id)
                                        val msg = if (nowFollowing) "${candidate.fullName}ga obuna bo'ldingiz ✓" else "Obuna bekor qilindi"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowed) {
                                            if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                        } else VelvetRosePrimary,
                                        contentColor = if (isFollowed) {
                                            if (isDark) Color(0xFFCBD5E1) else TextPrimary
                                        } else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = if (isFollowed) "Kuzatilmoqda" else "+ Kuzatish",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 2-BO'LIM: Hali Visolga qo'shilmagan kontaktlar (SMS / Share orqali taklif qilish)
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "VISOLGA TAKLIF QILISH (${unregisteredContacts.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF94A3B8) else TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    items(unregisteredContacts, key = { it.normalizedPhone }) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonOutline,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = contact.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color(0xFFE2E8F0) else TextPrimary
                                    )
                                    Text(
                                        text = contact.normalizedPhone,
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    sendSmsInvite(context, contact.normalizedPhone, contact.name)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, VelvetRosePrimary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = VelvetRosePrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Taklif",
                                    fontSize = 11.sp,
                                    color = VelvetRosePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Normalizes any phone string to E.164 standard format (+998...)
 */
private fun normalizePhoneNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when {
        digits.startsWith("998") && digits.length == 12 -> "+$digits"
        digits.length == 9 -> "+998$digits"
        digits.startsWith("8") && digits.length == 10 -> "+998${digits.substring(1)}"
        digits.isNotBlank() -> if (raw.startsWith("+")) raw else "+$digits"
        else -> ""
    }
}

/**
 * Sends a native SMS invitation to the selected contact
 */
private fun sendSmsInvite(context: Context, phone: String, name: String) {
    try {
        val inviteText = "Assalomu alaykum $name! Visol ilovasida yangi do'stlar va oila qurish imkoniyati bor. Qo'shiling: https://visol.uz/download"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phone")
            putExtra("sms_body", inviteText)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Visol ilovasiga qo'shiling: https://visol.uz/download")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Taklif yuborish"))
    }
}
