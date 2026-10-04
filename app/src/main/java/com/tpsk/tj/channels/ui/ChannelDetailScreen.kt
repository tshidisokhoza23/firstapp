package com.tpsk.tj.channels.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Report
import com.tpsk.tj.chat.model.Channel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings

import com.tpsk.tj.media.model.Post
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import coil.compose.AsyncImage
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.foundation.layout.imePadding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.tpsk.tj.chat.ui.components.ChatBottomBarUnique
import com.tpsk.tj.chat.ui.components.AttachmentPanel
import com.tpsk.tj.chat.ui.components.AttachmentPreview
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.chat.model.MessageType
import androidx.compose.material3.LinearProgressIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailScreen(
    channel: Channel,
    posts: List<Post>,
    currentUserId: String?,
    messages: List<com.tpsk.tj.chat.model.Message> = emptyList(),
    contacts: List<com.tpsk.tj.chat.model.Contact> = emptyList(),
    members: List<com.tpsk.tj.common.data.api.ApiUser> = emptyList(),
    onBack: () -> Unit,
    onSubscribe: (String) -> Unit = {},
    onMute: (String, Boolean) -> Unit = { _, _ -> },
    onCreatePost: () -> Unit = {},
    onSendPost: (String, List<String>) -> Unit = { _, _ -> },
    onAddAdmin: (String) -> Unit = {},
    onRemoveAdmin: (String) -> Unit = {},
    onToggleAdminPosting: (Boolean) -> Unit = {},
    onOpenDiscussion: () -> Unit = {},
    onReportChannel: (String, String) -> Unit = { _, _ -> },
    onShareStatus: () -> Unit = {},
    onSaveDescription: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showAddAdmin by remember { mutableStateOf(false) }
    var showUnfollowConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDescriptionDialog by remember { mutableStateOf(false) }
    var descriptionDraft by remember { mutableStateOf("") }
    if (showDescriptionDialog) {
        AlertDialog(
            onDismissRequest = { showDescriptionDialog = false },
            title = { Text("Add description") },
            text = {
                OutlinedTextField(
                    value = descriptionDraft,
                    onValueChange = { descriptionDraft = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    val text = descriptionDraft.trim()
                    if (text.isNotEmpty()) onSaveDescription(text)
                    showDescriptionDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                Button(onClick = { showDescriptionDialog = false }) { Text("Cancel") }
            }
        )
    }
    var postText by remember { mutableStateOf("") }
    var showAttachments by remember { mutableStateOf(false) }

    val displayPosts = remember(posts, messages) {
        val mappedMessages = messages.sortedBy { it.timestamp }.map { msg ->
            val timestampLabel = try {
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(msg.timestamp))
            } catch (_: Exception) {
                "19:08"
            }
            Post(
                id = msg.id,
                authorId = msg.senderId,
                authorName = channel.name,
                authorHandle = "@channel",
                authorAvatarUrl = channel.avatarUrl,
                authorAvatarColor = channel.avatarColor,
                content = msg.content,
                mediaUrls = msg.mediaUrls,
                timestamp = timestampLabel,
                likes = 0,
                repliesCount = 0
            )
        }
        (mappedMessages + posts).distinctBy { it.id }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            val urls = uris.map { it.toString() }
            onSendPost("", urls)
            showAttachments = false
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onSendPost("", listOf(it.toString()))
            showAttachments = false
        }
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onSendPost("Document update", listOf(it.toString()))
            showAttachments = false
        }
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onSendPost("Audio update", listOf(it.toString()))
            showAttachments = false
        }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            onSendPost("Video update", listOf(it.toString()))
            showAttachments = false
        }
    }

    val isCreator = currentUserId != null && channel.createdBy == currentUserId
    val isAdmin = channel.isAdmin || isCreator
    val canPost = isCreator || (channel.isAdmin && channel.adminsCanPost)
    val isFollower = channel.isSubscribed || isAdmin

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showInfo = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(channel.avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (channel.avatarUrl != null) {
                                AsyncImage(
                                    model = channel.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(channel.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    channel.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (channel.isVerified || isAdmin) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = "Verified",
                                        tint = Color(0xFF00A884),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                if (isAdmin) "Public channel" else "${channel.followers} followers",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (isFollower) {
                        IconButton(onClick = { onMute(channel.id, !channel.isMuted) }) {
                            Icon(
                                imageVector = if (channel.isMuted) Icons.Default.NotificationsOff else Icons.Default.NotificationsNone,
                                contentDescription = "Mute",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                    IconButton(onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out ${channel.name} channel on FirstApp!")
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Channel"))
                    }) {
                        Icon(Icons.Default.Link, "Share Link", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Channel info", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    showInfo = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (channel.isMuted) "Unmute notifications" else "Mute notifications",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    if (!isFollower) {
                                        Toast.makeText(context, "Follow the channel first", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onMute(channel.id, !channel.isMuted)
                                    }
                                }
                            )
                            if (isAdmin) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                DropdownMenuItem(
                                    text = { Text("Add Admin", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMenu = false
                                        showAddAdmin = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) }
                                )
                            }
                            if (isCreator) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Admins can post", color = MaterialTheme.colorScheme.onSurface)
                                            Switch(
                                                checked = channel.adminsCanPost,
                                                onCheckedChange = {
                                                    showMenu = false
                                                    onToggleAdminPosting(it)
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                                )
                                            )
                                        }
                                    },
                                    onClick = { }
                                )
                            }
                            if (isFollower && !isCreator) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (channel.isSubscribed) "Unfollow channel" else "Follow channel",
                                            color = if (channel.isSubscribed) Color.Red else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        if (channel.isSubscribed) showUnfollowConfirm = true else onSubscribe(channel.id)
                                    }
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            DropdownMenuItem(
                                text = { Text("Report channel", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Report, contentDescription = "Report Channel", tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            if (!isFollower) {
                // State A: Non-Follower Big Green Follow Button (Screenshot 3)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { onSubscribe(channel.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A884),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Text("Follow channel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "This channel has added privacy for your profile and phone number. ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Learn more",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00A884),
                                modifier = Modifier.clickable { }
                            )
                        }
                    }
                }
            } else if (channel.isMuted) {
                // State B: Muted Follower Unmute Banner (Screenshot 2)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Unmute to get notifications for this channel.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text("Not now", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }

                            Button(
                                onClick = { onMute(channel.id, false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE8F5E9),
                                    contentColor = Color(0xFF00A884)
                                ),
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Unmute", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else if (canPost) {
                // State C: Unmuted Follower / Admin Input Bar (Screenshot 1 & 4)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        if (showAttachments) {
                            AttachmentPanel(
                                onImageClick = { imageLauncher.launch("image/*") },
                                onVideoClick = { videoLauncher.launch("video/*") },
                                onDocClick = { documentLauncher.launch("*/*") },
                                onContactClick = {
                                    showAttachments = false
                                    Toast.makeText(context, "Select contact to share", Toast.LENGTH_SHORT).show()
                                },
                                onCameraClick = { cameraLauncher.launch("image/*") },
                                onAudioClick = { audioLauncher.launch("audio/*") },
                                onLocationClick = {
                                    showAttachments = false
                                    onSendPost("📍 Shared Location", emptyList())
                                },
                                onGifClick = {
                                    showAttachments = false
                                    onCreatePost()
                                }
                            )
                        }

                        ChatBottomBarUnique(
                            text = postText,
                            onTextChange = { postText = it },
                            onSend = {
                                if (postText.isNotBlank()) {
                                    onSendPost(postText.trim(), emptyList())
                                    postText = ""
                                    showAttachments = false
                                }
                            },
                            onAttachClick = { showAttachments = !showAttachments },
                            onEmojiClick = { },
                            onCameraClick = { cameraLauncher.launch("image/*") },
                            onMicClick = { },
                            placeholder = "Write an update"
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFECE5DD)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // System Privacy Notices & Start Growing Card (Screenshot 4)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            "((•)) This channel is public and visible to anyone. There is added privacy for your profile and phone number.",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp
                        ) {
                            Text(
                                "You created this channel, \"${channel.name}\"",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big White Start Growing Card (Screenshot 4)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(CircleShape)
                                            .background(channel.avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(channel.name.take(1).uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00A884)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    "Start growing \"${channel.name}\"",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .clickable { descriptionDraft = channel.description; showDescriptionDialog = true }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF00A884), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add description", color = Color(0xFF00A884), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                        .clickable { onShareStatus() },
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🎯 Share to my status", color = Color(0xFF00A884), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(24.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                        .clickable { showAddAdmin = true },
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF00A884), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Invite admins", color = Color(0xFF00A884), fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Regular Feed Posts
            items(displayPosts, key = { it.id }) { post ->
                ChannelPostItem(post)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddAdmin) {
        val available = contacts.filter { it.isRegistered && it.id != currentUserId }
        AlertDialog(
            onDismissRequest = { showAddAdmin = false },
            title = { Text("Add admin") },
            text = {
                if (available.isEmpty()) {
                    Text("No registered contacts to promote.")
                } else {
                    LazyColumn {
                        items(available, key = { it.id }) { contact ->
                            Text(
                                text = contact.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddAdmin(contact.id)
                                        showAddAdmin = false
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showAddAdmin = false }) { Text("Cancel") }
            }
        )
    }

    if (showUnfollowConfirm) {
        AlertDialog(
            onDismissRequest = { showUnfollowConfirm = false },
            title = { Text("Unfollow channel?") },
            text = { Text("You will stop seeing new posts from ${channel.name} in your chats.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showUnfollowConfirm = false
                    onSubscribe(channel.id)
                }) { Text("Unfollow", color = Color.Red) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showUnfollowConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showReportDialog) {
        var selectedReason by remember { mutableStateOf("Spam or misleading") }
        val reasons = listOf("Spam or misleading", "Inappropriate content", "Harassment or hate speech", "Copyright violation", "Other")

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Channel", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Help us understand the issue. Why are you reporting this channel?", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedReason == reason),
                                onClick = { selectedReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(reason, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportChannel(channel.id, selectedReason)
                        Toast.makeText(context, "Thank you. Report submitted for review.", Toast.LENGTH_LONG).show()
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Report", color = Color.White)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ChannelPostItem(
    post: Post,
    message: Message? = null,
    onMediaClick: (String, String) -> Unit = { _, _ -> }
) {
    val displayMsg = message ?: Message(
        id = post.id,
        conversationId = "",
        content = post.content,
        senderId = post.authorId,
        timestamp = System.currentTimeMillis(),
        isFromMe = false,
        type = when {
            post.content.startsWith("📍 Shared Location") -> MessageType.LOCATION
            post.content == "Voice Note" || post.content.startsWith("Audio update") -> MessageType.AUDIO_MESSAGE
            post.content.startsWith("Video update") -> MessageType.VIDEO
            post.content.startsWith("Document update") -> MessageType.DOCUMENT
            post.mediaUrls.isNotEmpty() -> MessageType.IMAGE
            else -> MessageType.TEXT
        },
        mediaUrls = post.mediaUrls
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(max = 380.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Media / Attachment Rendering
                if (displayMsg.mediaUrls.isNotEmpty() || displayMsg.type != MessageType.TEXT) {
                    AttachmentPreview(
                        message = displayMsg,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onMediaClick = onMediaClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Text Content
                val isPlaceholder = displayMsg.content == "Image" || displayMsg.content == "Video" ||
                        displayMsg.content == "Voice Note" || displayMsg.content == "Document" || displayMsg.content == "Shared Location" ||
                        displayMsg.content.startsWith("Document update") || displayMsg.content.startsWith("Video update") || displayMsg.content.startsWith("Audio update")
                if (displayMsg.content.isNotBlank() && !isPlaceholder) {
                    Text(
                        text = displayMsg.content,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Reaction Pills & Forward Bar (Screenshot 1-3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Emoji Reaction Pill (only shown if post has reactions)
                        if (post.likes > 0) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("❤️ 👍", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${post.likes}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Forward Pill (only shown if post has forwards/replies)
                        if (post.repliesCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = "Forward post",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${post.repliesCount}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Timestamp
                    Text(
                        text = post.timestamp.ifBlank { "19:08" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
