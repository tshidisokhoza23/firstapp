package com.example.firstapp.settings.ui.screens

import androidx.compose.animation.*
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
import coil.compose.AsyncImage
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.mingle.ui.MingleActivityNotification
import com.example.firstapp.mingle.ui.MingleActivityType
import com.example.firstapp.mingle.ui.MingleProfilePreviewDialog
import com.example.firstapp.settings.ui.viewmodel.NotificationSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Mingle Activity, 1: Settings
    var selectedNotificationForPreview by remember { mutableStateOf<MingleActivityNotification?>(null) }
    val contacts by userViewModel.contacts.collectAsState()

    val privateChatsEnabled by viewModel.privateChatsEnabled.collectAsState()
    val groupsEnabled by viewModel.groupsEnabled.collectAsState()
    val channelsEnabled by viewModel.channelsEnabled.collectAsState()
    val soundsEnabled by viewModel.soundsEnabled.collectAsState()
    val vibrateEnabled by viewModel.vibrateEnabled.collectAsState()
    val previewsEnabled by viewModel.previewsEnabled.collectAsState()

    // Generate Mingle activity notifications based on real contacts
    val mingleNotifications = remember(contacts) {
        val list = mutableListOf<MingleActivityNotification>()
        val cList = contacts.take(6)

        cList.getOrNull(0)?.let { c ->
            list.add(
                MingleActivityNotification(
                    id = "m_notif_1",
                    actorId = c.id,
                    actorName = c.displayName,
                    actorAvatarUrl = c.avatarUrl,
                    type = MingleActivityType.MINGLE_LIKE,
                    actionText = "liked your Mingle profile ❤️",
                    timeAgo = "2m ago"
                )
            )
        }

        cList.getOrNull(1)?.let { c ->
            list.add(
                MingleActivityNotification(
                    id = "m_notif_2",
                    actorId = c.id,
                    actorName = c.displayName,
                    actorAvatarUrl = c.avatarUrl,
                    type = MingleActivityType.MATCH,
                    actionText = "matched with you! Say hi 👋",
                    timeAgo = "15m ago"
                )
            )
        }

        cList.getOrNull(2)?.let { c ->
            list.add(
                MingleActivityNotification(
                    id = "m_notif_3",
                    actorId = c.id,
                    actorName = c.displayName,
                    actorAvatarUrl = c.avatarUrl,
                    type = MingleActivityType.PHOTO_LIKE,
                    actionText = "liked photo #2 on your profile 📸",
                    timeAgo = "45m ago",
                    photoUrl = "https://picsum.photos/200/300?random=11"
                )
            )
        }

        cList.getOrNull(3)?.let { c ->
            list.add(
                MingleActivityNotification(
                    id = "m_notif_4",
                    actorId = c.id,
                    actorName = c.displayName,
                    actorAvatarUrl = c.avatarUrl,
                    type = MingleActivityType.VIEW,
                    actionText = "viewed your Mingle profile 👁",
                    timeAgo = "2h ago"
                )
            )
        }

        cList.getOrNull(4)?.let { c ->
            list.add(
                MingleActivityNotification(
                    id = "m_notif_5",
                    actorId = c.id,
                    actorName = c.displayName,
                    actorAvatarUrl = c.avatarUrl,
                    type = MingleActivityType.SUPER_LIKE,
                    actionText = "sent you a Super Like ⭐",
                    timeAgo = "5h ago"
                )
            )
        }

        if (list.isEmpty()) {
            list.add(
                MingleActivityNotification(
                    id = "m_default_1",
                    actorId = "u1",
                    actorName = "Clientelle",
                    actorAvatarUrl = null,
                    type = MingleActivityType.MINGLE_LIKE,
                    actionText = "liked your Mingle profile ❤️",
                    timeAgo = "10m ago"
                )
            )
            list.add(
                MingleActivityNotification(
                    id = "m_default_2",
                    actorId = "u2",
                    actorName = "Sbg Number",
                    actorAvatarUrl = null,
                    type = MingleActivityType.MATCH,
                    actionText = "matched with you! Say hi 👋",
                    timeAgo = "1h ago"
                )
            )
        }

        list
    }

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
                actions = {
                    IconButton(onClick = { selectedTab = if (selectedTab == 0) 1 else 0 }) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Settings else Icons.Default.Notifications,
                            contentDescription = "Toggle Preferences",
                            tint = MaterialTheme.colorScheme.primary
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Activity", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Preferences", fontWeight = FontWeight.Bold) }
                )
            }

            Crossfade(targetState = selectedTab, label = "MingleNotifTab") { tab ->
                if (tab == 0) {
                    // Mingle Activity Notifications List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(mingleNotifications, key = { it.id }) { item ->
                            MingleActivityRow(
                                item = item,
                                onClick = { selectedNotificationForPreview = item }
                            )
                        }
                    }
                } else {
                    // Notification Settings Switches
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "Message & Mingle Notifications",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        NotificationSwitch("Private Chats", "Enabled", privateChatsEnabled) {
                            viewModel.setPrivateChatsEnabled(it)
                        }
                        NotificationSwitch("Groups", "Enabled", groupsEnabled) {
                            viewModel.setGroupsEnabled(it)
                        }
                        NotificationSwitch("Channels", "Enabled", channelsEnabled) {
                            viewModel.setChannelsEnabled(it)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Text(
                            "General",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        NotificationSwitch("Notification Sounds", "Play sounds for incoming messages", soundsEnabled) {
                            viewModel.setSoundsEnabled(it)
                        }
                        NotificationSwitch("Vibrate", "Vibrate for incoming messages", vibrateEnabled) {
                            viewModel.setVibrateEnabled(it)
                        }
                        NotificationSwitch("Message Previews", "Show preview text in notifications", previewsEnabled) {
                            viewModel.setPreviewsEnabled(it)
                        }
                    }
                }
            }
        }

        selectedNotificationForPreview?.let { item ->
            MingleProfilePreviewDialog(
                item = item,
                onDismiss = { selectedNotificationForPreview = null },
                onNavigateToChat = { actorId ->
                    selectedNotificationForPreview = null
                    onNavigateToChat(actorId)
                }
            )
        }
    }
}

@Composable
private fun MingleActivityRow(
    item: MingleActivityNotification,
    onClick: () -> Unit = {}
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

@Composable
fun NotificationSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}
