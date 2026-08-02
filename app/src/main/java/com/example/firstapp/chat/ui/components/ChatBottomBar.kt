package com.example.firstapp.chat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun ChatBottomBarUnique(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onMicClick: () -> Unit,
    isRecording: Boolean = false,
    recordingDuration: Int = 0,
    enterIsSend: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 12.dp, top = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            modifier = Modifier.weight(1f).padding(bottom = 2.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                if (isRecording) {
                    Icon(
                        Icons.Default.Mic,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(14.dp).size(20.dp)
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%d:%02d", recordingDuration / 60, recordingDuration % 60),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Slide to cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                } else {
                    IconButton(onClick = onEmojiClick) {
                        Icon(Icons.Default.SentimentSatisfiedAlt, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    TextField(
                        value = text,
                        onValueChange = onTextChange,
                        placeholder = { Text("Message...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (enterIsSend) ImeAction.Send else ImeAction.Default
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = { if (text.isNotBlank()) onSend() }
                        )
                    )
                    
                    IconButton(onClick = onAttachClick) {
                        Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(45f))
                    }
                    
                    if (text == "") {
                        IconButton(onClick = { /* Camera handled in ChatScreen */ }) {
                            Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Circular button (Send/Mic) - Unique style
        Surface(
            modifier = Modifier
                .size(52.dp)
                .clickable { if (text.isNotBlank()) onSend() else onMicClick() },
            shape = CircleShape,
            color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (text.isNotBlank()) Icons.AutoMirrored.Filled.Send else if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
