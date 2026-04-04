package com.example.firstapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.ChatMessage
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.AppTextPrimary
import com.example.firstapp.ui.theme.AppTextSecondary

@Composable
fun ChatItem(chat: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Spacer(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(chat.avatarColor)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.senderName,
                color = AppTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = chat.lastMessage,
                color = AppTextSecondary,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = chat.time,
                color = if (chat.unreadCount > 0) AppAccent else AppTextSecondary,
                fontSize = 12.sp
            )
            if (chat.unreadCount > 0) {
                Spacer(modifier = Modifier.size(4.dp))
                BoxWithText(
                    text = chat.unreadCount.toString(),
                    backgroundColor = AppAccent,
                    textColor = AppBackground
                )
            }
        }
    }
}

@Composable
fun BoxWithText(text: String, backgroundColor: Color, textColor: Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
