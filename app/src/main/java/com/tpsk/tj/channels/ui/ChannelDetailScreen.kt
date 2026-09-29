package com.tpsk.tj.channels.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Download
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
import androidx.compose.material.icons.filled.PermMedia
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
import com.tpsk.tj.common.ui.theme.AppBackground
import com.tpsk.tj.common.ui.theme.AppSecondary
import com.tpsk.tj.common.ui.theme.AppTextPrimary
import com.tpsk.tj.common.ui.theme.AppTextSecondary
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.FloatingActionButton
import com.tpsk.tj.common.ui.theme.AppAccent

import com.tpsk.tj.media.model.Post
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import coil.compose.AsyncImage
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.foundation.layout.imePadding
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.tpsk.tj.chat.ui.components.ChatBottomBarUnique
import com.tpsk.tj.chat.ui.components.AttachmentPanel
import com.tpsk.tj.chat.ui.components.AttachmentPreview
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.chat.model.MessageType

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
    onReportChannel: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showAddAdmin by remember { mutableStateOf(false) }
    var showUnfollowConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var postText by remember { mutableStateOf("") }
    var showAttachments by remember { mutableStateOf(false) }

    val displayPosts = remember(posts, messages) {
        val mappedMessages = messages.sortedBy { it.timestamp }.map { msg ->
            val timestampLabel = try {
                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                sdf.format(java.util.Date(msg.timestamp))
            } catch (_: Exception) {
                "Just now"
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
                                if (channel.isAdmin && !channel.isVerified) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                "${channel.followers} followers", 
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
                    if (isCreator || isAdmin) {
                        IconButton(onClick = { 
                            Toast.makeText(context, "Channel link copied", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Link, contentDescription = "Channel link", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    } else {
                        IconButton(onClick = {
                            if (isFollower) {
                                onMute(channel.id, !channel.isMuted)
                            } else {
                                Toast.makeText(context, "Follow the channel first", Toast.LENGTH_SHORT).show()
                            }
                        }) {
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
                        Icon(Icons.Default.Share, "Share Channel", tint = MaterialTheme.colorScheme.onBackground) 
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                onSubscribe(channel.id)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onBackground,
                                contentColor = MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Follow channel", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            } else if (canPost) {
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Channel Info Header
                if (showInfo) {
                Text(
                    text = "About this channel",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Text(
                    text = channel.description.takeIf { it.isNotBlank() }
                        ?: "Broadcast updates from ${channel.name}. Followers can view posts; only the owner and allowed admins can publish.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when {
                        isCreator -> "You own this channel. You can post, add admins, and decide whether admins may post."
                        isAdmin && canPost -> "You are an admin and can publish updates."
                        isAdmin -> "You are an admin. Only the owner can post until posting is enabled."
                        isFollower -> "You follow this channel. Followers cannot post in a broadcast channel."
                        else -> "Follow to receive updates. This is not a group chat."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))

                if (isAdmin) {
                    Text(
                        text = "Admins",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Followers cannot post. Only the owner and allowed admins can publish updates.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val adminIds = buildList {
                        channel.createdBy?.takeIf { it.isNotBlank() }?.let { add(it) }
                        if (isAdmin && currentUserId != null && currentUserId != channel.createdBy) add(currentUserId)
                    }.distinct()
                    adminIds.forEach { adminId ->
                        val contact = contacts.find { it.id == adminId }
                        val member = members.find { it.id == adminId }
                        val name = when {
                            adminId == currentUserId -> "You"
                            !contact?.displayName.isNullOrBlank() -> contact!!.displayName
                            !member?.display_name.isNullOrBlank() -> member!!.display_name
                            else -> adminId
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Medium)
                                Text(
                                    if (adminId == channel.createdBy) "Owner" else "Admin",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            if (isCreator && adminId != currentUserId && adminId != channel.createdBy) {
                                Text(
                                    "Remove",
                                    color = Color.Red,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { onRemoveAdmin(adminId) }
                                )
                            }
                        }
                    }
                    if (isCreator || isAdmin) {
                        Text(
                            "Add admin",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showAddAdmin = true }
                                .padding(vertical = 8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(24.dp))
                }
                }

                Text(
                    text = "Updates",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (displayPosts.isEmpty()) {
                item {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PermMedia, 
                                null, 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), 
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No updates yet", 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (canPost) {
                                Text(
                                    "Share something with your followers", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), 
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else if (!isFollower) {
                                Text(
                                    "Follow to see updates from this channel",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                items(displayPosts, key = { it.id }) { post ->
                    ChannelPostItem(post)
                }
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
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 380.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Media / Attachment Rendering (Image, Video, Audio, Document, Location)
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

                // Footer with reactions, timestamp, and forward share button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("❤️ ${post.likes}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.timestamp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Forward post",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChannelDetailScreenPreview() {
    val mockChannel = Channel(
        id = "1",
        name = "Android Developers",
        description = "The official channel for Android developers.",
        followers = "1.2M",
        avatarUrl = null,
        avatarColor = Color.Blue,
        isVerified = true,
        isAdmin = true,
        adminsCanPost = true,
        createdBy = "user1"
    )
    MaterialTheme {
        ChannelDetailScreen(
            channel = mockChannel,
            posts = emptyList(),
            currentUserId = "user1",
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChannelDetailScreenAdminPreview() {
    val mockChannel = Channel(
        id = "1",
        name = "Android Developers",
        description = "The official channel for Android developers.",
        followers = "1.2M",
        avatarUrl = null,
        avatarColor = Color.Blue,
        isVerified = true,
        isAdmin = true,
        adminsCanPost = false,
        createdBy = "creator_id"
    )
    MaterialTheme {
        ChannelDetailScreen(
            channel = mockChannel,
            posts = emptyList(),
            currentUserId = "admin_id",
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChannelDetailScreenWithPostsPreview() {
    val mockChannel = Channel(
        id = "1",
        name = "Android Developers",
        description = "The official channel for Android developers.",
        followers = "1.2M",
        avatarUrl = null,
        avatarColor = Color(0xFF2196F3),
        isVerified = true,
        isAdmin = true,
        adminsCanPost = true,
        createdBy = "user1"
    )
    val mockPosts = listOf(
        Post(
            id = "p1",
            authorId = "user1",
            authorName = "Android Developers",
            authorHandle = "@androiddev",
            authorAvatarUrl = null,
            authorAvatarColor = Color(0xFF2196F3),
            content = "Welcome to the official Android Developers channel! Stay tuned for news, updates, tips, and tutorials directly from the Android team.",
            mediaUrls = emptyList(),
            timestamp = "10:30 AM",
            likes = 1240,
            repliesCount = 85
        ),
        Post(
            id = "p2",
            authorId = "user1",
            authorName = "Android Developers",
            authorHandle = "@androiddev",
            authorAvatarUrl = null,
            authorAvatarColor = Color(0xFF2196F3),
            content = "Jetpack Compose 1.7 is now stable! Check out performance improvements, lazy list optimizations, and new animation APIs.",
            mediaUrls = emptyList(),
            timestamp = "2:15 PM",
            likes = 3450,
            repliesCount = 210
        )
    )
    MaterialTheme {
        ChannelDetailScreen(
            channel = mockChannel,
            posts = mockPosts,
            currentUserId = "user1",
            onBack = {}
        )
    }
}
