package com.example.firstapp.ui.components

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
import com.example.firstapp.model.Short

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortMoreMenuSheet(
    short: Short,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    text = "More Options",
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
            MoreMenuItem(
                icon = Icons.Default.ContentCopy,
                text = "Copy link",
                onClick = { onAction("copy_link"); onDismiss() }
            )
            MoreMenuItem(
                icon = Icons.Default.ClosedCaption,
                text = "Captions",
                onClick = { onAction("captions"); onDismiss() }
            )
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
