package com.tpsk.tj.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.common.ui.theme.AppAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportReasonDialog(
    targetTitle: String = "Report Post",
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val reportReasons = listOf(
        "Spam or misleading",
        "Inappropriate or explicit content",
        "Harassment or hate speech",
        "Violence or dangerous activity",
        "Copyright or intellectual property violation",
        "Other"
    )

    var selectedReason by remember { mutableStateOf(reportReasons[0]) }
    var additionalDetails by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E2124),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = targetTitle,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Why are you reporting this content?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                reportReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (reason == selectedReason),
                                onClick = { selectedReason = reason }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (reason == selectedReason),
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AppAccent,
                                unselectedColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                if (selectedReason == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = additionalDetails,
                        onValueChange = { additionalDetails = it },
                        placeholder = { Text("Describe the issue...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = AppAccent
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalReason = if (selectedReason == "Other" && additionalDetails.isNotBlank()) {
                        "Other: ${additionalDetails.trim()}"
                    } else selectedReason
                    onSubmit(finalReason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
            ) {
                Text("Submit Report", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}
