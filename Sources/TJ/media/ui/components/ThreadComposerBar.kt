package com.tpsk.tj.media.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThreadComposerBar(
    onSend: (String) -> Unit,
    replyToName: String = "",
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = if (replyToName.isNotBlank()) {
                                    "Reply to $replyToName…"
                                } else {
                                    "Add a reply…"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSend(text.trim())
                        text = ""
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send reply",
                    tint = if (text.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
