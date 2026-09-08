package com.example.firstapp.channels.ui

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
import androidx.compose.material3.Surface
import com.example.firstapp.chat.model.Channel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.theme.AppTextPrimary
import com.example.firstapp.common.ui.theme.AppTextSecondary
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.FloatingActionButton
import com.example.firstapp.common.ui.theme.AppAccent

import com.example.firstapp.media.model.Post
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import coil.compose.AsyncImage
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelDetailScreen(
    channel: Channel,
    posts: List<Post>,
    currentUserId: String?,
    contacts: List<com.example.firstapp.chat.model.Contact> = emptyList(),
    members: List<com.example.firstapp.common.data.api.ApiUser> = emptyList(),
    onBack: () -> Unit,
    onSubscribe: (String) -> Unit = {},
    onMute: (String, Boolean) -> Unit = { _, _ -> },
    onCreatePost: () -> Unit = {},
    onAddAdmin: (String) -> Unit = {},
    onRemoveAdmin: (String) -> Unit = {},
    onToggleAdminPosting: (Boolean) -> Unit = {},
    onOpenDiscussion: () -> Unit = {}
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showAddAdmin by remember { mutableStateOf(false) }
    var showUnfollowConfirm by remember { mutableStateOf(false) }

    val isCreator = currentUserId != null && channel.createdBy == currentUserId
    val isAdmin = channel.isAdmin || isCreator
    val canPost = isCreator || (channel.isAdmin && channel.adminsCanPost)
    val isFollower = channel.isSubscribed || isAdmin

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                if (channel.isAdmin) {
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
                    IconButton(onClick = { /* Share */ }) { 
                        Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.onBackground) 
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) { 
                            Icon(Icons.Default.MoreVert, "More", tint = MaterialTheme.colorScheme.onBackground) 
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open discussion", color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    if (isFollower) onOpenDiscussion()
                                    else Toast.makeText(context, "Follow this channel first", Toast.LENGTH_SHORT).show()
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
        floatingActionButton = {
            if (canPost) {
                FloatingActionButton(
                    onClick = onCreatePost,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Post to Channel")
                }
            }
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

                // Recent Updates Title
                Text(
                    text = "Recent updates",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (posts.isEmpty()) {
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
                items(posts, key = { it.id }) { post ->
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
}

@Composable
fun ChannelPostItem(post: Post) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Centered Post Media Attachment
                if (post.mediaUrls.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = post.mediaUrls.first(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    if (post.content.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Centered Text Content
                if (post.content.isNotBlank()) {
                    Text(
                        text = post.content,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Centered Reactions & Timestamp Footer
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("❤️ ${post.likes}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("💬 ${post.repliesCount}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = post.timestamp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
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
