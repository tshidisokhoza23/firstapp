package com.tpsk.tj.media.ui.screens.shorts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.media.model.Post

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortMoreMenuSheet(
    short: com.tpsk.tj.media.model.Post,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    val currentUser = com.tpsk.tj.LocalCurrentUser.current
    val isOwner = currentUser != null && short.authorId == currentUser.id

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Options",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            MoreMenuItem(
                icon = Icons.Default.AddCircleOutline,
                text = "Add Short",
                onClick = { onAction("add_short"); onDismiss() }
            )

            if (!isOwner) {
                MoreMenuItem(
                    icon = Icons.Default.Mail,
                    text = "Message ${short.authorName}",
                    onClick = { onAction("message"); onDismiss() }
                )
            }
            
            MoreMenuItem(
                icon = Icons.Default.Download,
                text = "Save video",
                onClick = { onAction("save"); onDismiss() }
            )

            MoreMenuItem(
                icon = Icons.Default.ContentCopy,
                text = "Copy link",
                onClick = { onAction("copy_link"); onDismiss() }
            )

            if (!isOwner) {
                MoreMenuItem(
                    icon = Icons.Default.Block,
                    text = "Not interested",
                    onClick = { onAction("not_interested"); onDismiss() }
                )
                MoreMenuItem(
                    icon = Icons.Default.Report,
                    text = "Report",
                    onClick = { onAction("report"); onDismiss() },
                    contentColor = Color.Red
                )
            }
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    contentColor: Color = Color.White
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, color = contentColor, fontSize = 16.sp)
        }
    }
}
