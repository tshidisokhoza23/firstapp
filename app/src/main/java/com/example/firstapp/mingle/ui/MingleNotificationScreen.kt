package com.example.firstapp.mingle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.firstapp.mingle.ui.viewmodel.MingleViewModel
import com.example.firstapp.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MingleNotificationScreen(
    navController: NavController,
    onBack: () -> Unit,
    mingleViewModel: MingleViewModel = hiltViewModel()
) {
    val uiState by mingleViewModel.uiState.collectAsState()
    val mingleNotifications = uiState.activities

    LaunchedEffect(Unit) {
        mingleViewModel.loadActivity()
    }

    var selectedNotificationForPreview by remember { mutableStateOf<MingleActivityNotification?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mingle Activity", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            com.example.firstapp.common.ui.components.BannerAdView(
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(mingleNotifications, key = { it.id }) { item ->
                MingleActivityRow(
                    item = item,
                    onClick = {
                        selectedNotificationForPreview = item
                    }
                )
            }
        }

        selectedNotificationForPreview?.let { item ->
            MingleProfilePreviewDialog(
                item = item,
                onDismiss = { selectedNotificationForPreview = null },
                onNavigateToChat = { conversationId ->
                    selectedNotificationForPreview = null
                    navController.navigate(Screen.Chat.createRoute(conversationId, "mingle"))
                }
            )
        }
    }
}

@Composable
private fun MingleActivityRow(
    item: MingleActivityNotification,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Actor Avatar with Badge Overlay
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val formatted = com.example.firstapp.common.util.MediaUtils.formatUrl(item.actorAvatarUrl)
                    if (formatted != null) {
                        AsyncImage(
                            model = formatted,
                            contentDescription = item.actorName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = item.actorName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Type Badge
                val badgeColor = when (item.type) {
                    MingleActivityType.MINGLE_LIKE -> Color(0xFFE91E63)
                    MingleActivityType.MATCH -> Color(0xFF2196F3)
                    MingleActivityType.VIEW -> Color(0xFFFF9800)
                    MingleActivityType.PHOTO_LIKE -> Color(0xFF9C27B0)
                    MingleActivityType.SUPER_LIKE -> Color(0xFFFFC107)
                }

                val badgeIcon = when (item.type) {
                    MingleActivityType.MINGLE_LIKE -> Icons.Default.Favorite
                    MingleActivityType.MATCH -> Icons.Default.ChatBubble
                    MingleActivityType.VIEW -> Icons.Default.RemoveRedEye
                    MingleActivityType.PHOTO_LIKE -> Icons.Default.PhotoCamera
                    MingleActivityType.SUPER_LIKE -> Icons.Default.Star
                }

                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(badgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(item.actorName)
                        }
                        append(" ")
                        append(item.actionText)
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.timeAgo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!item.photoUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val formattedTarget = com.example.firstapp.common.util.MediaUtils.formatUrl(item.photoUrl)
                    AsyncImage(
                        model = formattedTarget,
                        contentDescription = "Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
