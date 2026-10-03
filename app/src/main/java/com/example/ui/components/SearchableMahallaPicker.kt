package com.example.ui.components

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UzbekistanData
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableMahallaPickerBottomSheet(
    regionName: String?,
    districtName: String?,
    selectedMahalla: String,
    onMahallaSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var customMahallaInput by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    var showCustomMahallaDialog by remember { mutableStateOf(false) }

    // Fetch all mahallas for district without any LIMIT or constraint
    val allMahallas = remember(regionName, districtName) {
        UzbekistanData.getMahallasForDistrict(regionName, districtName)
    }

    val filteredMahallas = remember(allMahallas, searchQuery) {
        if (searchQuery.isBlank()) allMahallas
        else {
            val q = searchQuery.trim().lowercase()
            allMahallas.filter { it.lowercase().contains(q) }
        }
    }

    // Brief simulated loading on first open
    LaunchedEffect(districtName) {
        isLoading = true
        delay(120)
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF1E142B),
        contentColor = Color(0xFFFDF4F5),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = SoftPeach.copy(alpha = 0.5f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mahallani tanlang (MFY)",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFDF4F5)
                    )
                    Text(
                        text = "${districtName ?: "Tuman"} • Jami ${allMahallas.size} ta rasmiy mahalla",
                        fontSize = 12.sp,
                        color = SoftPeach,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x20FFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Yopish",
                        tint = SoftPeach,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Real-time Search Input Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mahalla_search_input"),
                placeholder = {
                    Text(
                        "Mahalla nomini qidiring (masalan: Mustaqillik)...",
                        fontSize = 13.sp,
                        color = Color(0x88D4B8C7)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Qidiruv",
                        tint = SoftPeach,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Tozalash",
                                tint = SoftPeach,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WarmRosePink,
                    unfocusedBorderColor = Color(0x40FF8C61),
                    focusedContainerColor = Color(0x28261936),
                    unfocusedContainerColor = Color(0x18FFFFFF),
                    focusedTextColor = Color(0xFFFDF4F5),
                    unfocusedTextColor = Color(0xFFFDF4F5)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Mahalla Entry Section Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showCustomInput = !showCustomInput }
                    .background(Color(0x30FF5E7E))
                    .border(1.dp, Brush.linearGradient(listOf(WarmRosePink.copy(alpha = 0.5f), SoftPeach.copy(alpha = 0.5f))), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EditLocation,
                        contentDescription = null,
                        tint = SoftPeach,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Mahallangiz ro'yxatda yo'qmi? Qo'lda kiriting",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFDF4F5)
                    )
                }
                Icon(
                    imageVector = if (showCustomInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = SoftPeach,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = showCustomInput) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customMahallaInput,
                            onValueChange = { customMahallaInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_mahalla_input"),
                            placeholder = { Text("Mahalla nomini yozing...", fontSize = 13.sp, color = Color(0x88D4B8C7)) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarmRosePink,
                                unfocusedBorderColor = Color(0x40FF8C61),
                                focusedContainerColor = Color(0x28261936),
                                unfocusedContainerColor = Color(0x18FFFFFF),
                                focusedTextColor = Color(0xFFFDF4F5),
                                unfocusedTextColor = Color(0xFFFDF4F5)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customMahallaInput.isNotBlank()) {
                                    val formatted = if (customMahallaInput.endsWith(" MFY", ignoreCase = true)) {
                                        customMahallaInput.trim()
                                    } else {
                                        "${customMahallaInput.trim()} MFY"
                                    }
                                    onMahallaSelected(formatted)
                                    onDismiss()
                                }
                            },
                            enabled = customMahallaInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmRosePink)
                        ) {
                            Text("Qo'shish", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main List or Empty State
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SoftPeach, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Mahallalar yuklanmoqda...",
                            fontSize = 13.sp,
                            color = Color(0xFFD4B8C7)
                        )
                    }
                }
            } else if (filteredMahallas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = SoftPeach.copy(alpha = 0.6f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "\"$searchQuery\" bo'yicha mahalla topilmadi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFDF4F5)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Yuqoridagi 'Qo'lda kiriting' orqali mahallangizni yozishingiz mumkin.",
                            fontSize = 12.sp,
                            color = Color(0xFFD4B8C7),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showCustomMahallaDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmRosePink)
                        ) {
                            Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mahallangizni topa olmadingizmi? [ + Qo'lda kiritish ]", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(filteredMahallas, key = { it }) { mahallaName ->
                        val isSelected = mahallaName.equals(selectedMahalla, ignoreCase = true)

                        Surface(
                            onClick = {
                                onMahallaSelected(mahallaName)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0x35FF5E7E) else Color(0x0CFFFFFF),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SoftPeach else Color(0x18FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mahallaName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) SoftPeach else Color(0xFFFDF4F5)
                                    ),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Tanlangan",
                                        tint = SoftPeach,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showCustomMahallaDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SoftPeach),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftPeach),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mahallangizni topa olmadingizmi? [ + Qo'lda kiritish ]", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showCustomMahallaDialog) {
            CustomMahallaDialog(
                initialQuery = searchQuery,
                onDismiss = { showCustomMahallaDialog = false },
                onSaveCustomMahalla = { customName ->
                    showCustomMahallaDialog = false
                    val formatted = if (customName.endsWith(" MFY", ignoreCase = true)) customName else "$customName MFY"
                    onMahallaSelected(formatted)
                    onDismiss()
                }
            )
        }
    }
}
