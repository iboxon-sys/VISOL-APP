package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CustomMahallaDialog(
    initialQuery: String,
    onDismiss: () -> Unit,
    onSaveCustomMahalla: (String) -> Unit
) {
    var mahallaName by remember { mutableStateOf(initialQuery) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E142B),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddLocation, contentDescription = null, tint = SoftPeach)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Qo'lda mahalla kiritish",
                    color = Color(0xFFFDF4F5),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Ro'yxatdan topilmagan chekka mahalla yoki MFY nomini kiriting. U admin tomonidan tasdiqlanib bazaga qo'shiladi.",
                    color = Color(0xFFD4B8C7),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = mahallaName,
                    onValueChange = {
                        mahallaName = it
                        errorMessage = null
                    },
                    placeholder = { Text("Masalan: Bog'ishamol MFY", color = Color(0x88D4B8C7)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0x28261936),
                        unfocusedContainerColor = Color(0x18FFFFFF),
                        focusedBorderColor = WarmRosePink,
                        unfocusedBorderColor = Color(0x40FF8C61),
                        focusedTextColor = Color(0xFFFDF4F5),
                        unfocusedTextColor = Color(0xFFFDF4F5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, color = WarmRosePink, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (mahallaName.trim().length < 2) {
                        errorMessage = "Mahalla nomi kamida 2 ta harfdan iborat bo'lishi kerak"
                    } else {
                        onSaveCustomMahalla(mahallaName.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmRosePink),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Saqlash va qo'shish", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0x40FF8C61))
            ) {
                Text("Bekor qilish", color = Color(0xFFD4B8C7))
            }
        }
    )
}
