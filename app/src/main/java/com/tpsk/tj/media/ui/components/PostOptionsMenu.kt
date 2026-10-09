package com.tpsk.tj.media.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun PostOptionsMenu(
    post: Post,
    isOwner: Boolean,
    onReport: () -> Unit,
    onBlockUser: () -> Unit,
    onDelete: () -> Unit,
    onTurnOffComments: () -> Unit = {},
    onShare: () -> Unit,
    onCopyLink: () -> Unit = {},
    onBookmark: () -> Unit,
    onNotInterested: (() -> Unit)? = null,
    onContext: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 12.dp)
        ) {
            if (isOwner) {
                // Owner actions
                MenuActionItem(
                    icon = Icons.Outlined.Delete,
                    label = "Delete Post",
                    tint = Color(0xFFFF5252),
                    onClick = {
                        onDelete()
                        onDismiss()
                    }
                )
                MenuActionItem(
                    icon = Icons.Outlined.ModeComment,
                    label = if (post.repliesLocked) "Turn on comments" else "Turn off comments",
                    onClick = {
                        onTurnOffComments()
                        onDismiss()
                    }
                )
            } else {
                // Viewer actions (App Store & Google Play UGC Compliance mandatory Levers)
                MenuActionItem(
                    icon = if (post.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    label = if (post.isBookmarked) "Remove Bookmark" else "Bookmark",
                    onClick = {
                        onBookmark()
                        onDismiss()
                    }
                )
                if (onNotInterested != null) {
                    MenuActionItem(
                        icon = Icons.Outlined.VisibilityOff,
                        label = "Not interested in this post",
                        onClick = {
                            onNotInterested()
                            onDismiss()
                        }
                    )
                }
                MenuActionItem(
                    icon = Icons.Outlined.PersonOff,
                    label = "Block @${post.authorHandle}",
                    tint = Color(0xFFFFB74D),
                    onClick = {
                        onBlockUser()
                        onDismiss()
                    }
                )
                MenuActionItem(
                    icon = Icons.Outlined.ReportProblem,
                    label = "Report content",
                    tint = Color(0xFFFF5252),
                    onClick = {
                        onDismiss()
                        onReport()
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            if (onContext != null) {
                MenuActionItem(
                    icon = Icons.Outlined.Info,
                    label = "Notes, edits, and reputation",
                    onClick = {
                        onContext()
                        onDismiss()
                    }
                )
            }
            MenuActionItem(
                icon = Icons.Outlined.Share,
                label = "Share via...",
                onClick = {
                    onShare()
                    onDismiss()
                }
            )
            MenuActionItem(
                icon = Icons.Outlined.Link,
                label = "Copy link",
                onClick = {
                    onCopyLink()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun MenuActionItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = tint
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportContentDialog(
    post: Post,
    onDismiss: () -> Unit,
    onSubmitReport: (category: String, details: String) -> Unit
) {
    val reportCategories = remember {
        listOf(
            "Child Sexual Abuse Material (CSAM) / Child Exploitation",
            "Harassment or Bullying",
            "Hate Speech or Discrimination",
            "Violence or Dangerous Organizations",
            "Self-Harm or Suicide",
            "Spam, Scams, or Phishing",
            "Intellectual Property / Copyright Infringement"
        )
    }

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var additionalDetails by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Report Content",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Why are you reporting this post by @${post.authorHandle}?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            reportCategories.forEach { category ->
                val isSelected = selectedCategory == category
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedCategory = category },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedCategory = category }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = category,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (selectedCategory != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = additionalDetails,
                    onValueChange = { additionalDetails = it },
                    label = { Text("Additional details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val cat = selectedCategory ?: return@Button
                        onSubmitReport(cat, additionalDetails)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text(
                        text = "Submit Report",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
