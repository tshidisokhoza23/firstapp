package com.tpsk.tj.media.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.media.model.SocialActionType
import com.tpsk.tj.media.model.SocialNotificationItem
import com.tpsk.tj.media.ui.viewmodel.SocialNotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialNotificationsScreen(
    onBackClick: () -> Unit,
    onPostClick: (postId: String) -> Unit,
    onUserProfileClick: (userId: String) -> Unit,
    viewModel: SocialNotificationsViewModel = hiltViewModel()
) {
    val notifications by viewModel.notificationsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadNotifications() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading && notifications.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null && notifications.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = errorMessage ?: "Failed to load notifications",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadNotifications() }) {
                            Text("Retry")
                        }
                    }
                }
                notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No social notifications yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(notifications, key = { it.id }) { item ->
                            SocialNotificationRow(
                                item = item,
                                onClick = {
                                    viewModel.markAsRead(item.id)
                                    when (item.actionType) {
                                        SocialActionType.FOLLOW -> onUserProfileClick(item.actorId)
                                        else -> item.targetPostId?.let { onPostClick(it) }
                                    }
                                }
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialNotificationRow(
    item: SocialNotificationItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (item.isRead) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (actionIcon, actionColor) = when (item.actionType) {
            SocialActionType.LIKE -> Icons.Default.Favorite to Color(0xFFE91E63)
            SocialActionType.REPLY -> Icons.AutoMirrored.Filled.Comment to Color(0xFF2196F3)
            SocialActionType.REPOST -> Icons.Default.Repeat to Color(0xFF4CAF50)
            SocialActionType.FOLLOW -> Icons.Default.PersonAdd to Color(0xFF9C27B0)
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(actionColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = actionColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(item.actorName)
                    }
                    append(" ")
                    append(item.actionText)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (!item.postSnippet.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.postSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = item.timeAgo,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
