@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.firstapp.chat.ui.screens

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.MessageType
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.chat.ui.components.ChatAvatar
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.SecondaryText
import com.example.firstapp.common.util.MediaUtils
import com.example.firstapp.common.util.Resource

@Composable
fun ChatProfileScreen(
    chat: ChatMessage,
    onBack: () -> Unit,
    onMediaClick: (String, String) -> Unit,
    onNavigateToSharedMedia: (String) -> Unit = {},
    sharedMedia: List<Message> = emptyList(),
    groupInfo: Resource<com.example.firstapp.chat.data.api.GroupInfoResponse> = Resource.Loading(),
    contacts: List<Contact> = emptyList(),
    onClearChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onMuteToggle: (Boolean) -> Unit = {},
    onAddMember: (String) -> Unit = {},
    onRemoveMember: (String) -> Unit = {},
    onPromoteAdmin: (String) -> Unit = {},
    onLeaveGroup: () -> Unit = {},
    onToggleAdminPosting: (Boolean) -> Unit = {},
    onSubscribe: (String) -> Unit = {},
    onAvatarSelected: (android.net.Uri) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    var showAddMemberDialog by remember { mutableStateOf(false) }
    val currentUser = com.example.firstapp.LocalCurrentUser.current
    val currentUserId = currentUser.id
    val isChannel = chat.type == ConversationType.CHANNEL
    val isCreator = chat.createdBy != null && chat.createdBy == currentUserId
    
    // Can edit if it's my own profile or I'm an admin of the group/channel
    val canEditAvatar = remember(chat.id, chat.isAdmin, currentUserId) {
        chat.id == currentUserId || chat.isAdmin
    }

    val pickMedia = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        uri?.let { onAvatarSelected(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isChannel) {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val deepLink = "firstapp://channel/${chat.id}"
                                putExtra(Intent.EXTRA_SUBJECT, "Check out this channel: ${chat.senderName}")
                                putExtra(Intent.EXTRA_TEXT, "Follow ${chat.senderName} on TJ: $deepLink")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Channel"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Channel", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header: Large Avatar & Name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .background(Color.DarkGray)
            ) {
                ChatAvatar(
                    imageUrl = chat.avatarUrl,
                    name = chat.senderName,
                    size = 350.dp,
                    fontSize = 120.sp,
                    backgroundColor = Color.DarkGray,
                    textColor = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (canEditAvatar) Modifier.clickable {
                                pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else Modifier
                        )
                )

                if (canEditAvatar) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AppAccent)
                            .clickable {
                                pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                
                // Name Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = chat.senderName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (chat.phoneNumber != null) {
                            Text(
                                text = chat.phoneNumber,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Info Section
            Column(modifier = Modifier.padding(16.dp)) {
                if (isChannel) {
                    Text(
                        "Channel info",
                        color = AppAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "This is a broadcast channel for ${chat.senderName}.", 
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, null, tint = SecondaryText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${chat.followers} followers",
                            color = SecondaryText,
                            fontSize = 14.sp
                        )
                    }
                } else if (chat.isGroup) {
                    Text(
                        "Group info",
                        color = AppAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Group conversation", 
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text(
                        "Details",
                        color = AppAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val detailValue = chat.phoneNumber ?: chat.id
                    val isUuid = remember(detailValue) {
                        detailValue.lowercase().matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
                    }
                    
                    if (chat.phoneNumber != null || !isUuid) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(
                                imageVector = if (chat.phoneNumber != null) Icons.Default.Phone else Icons.Default.AlternateEmail, 
                                contentDescription = null, 
                                tint = AppAccent, 
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = detailValue,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (chat.phoneNumber != null) "Mobile" else "Username",
                                    color = SecondaryText,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        // HIDDEN: Don't show raw UUIDs in the Details section.
                        // Instead, maybe show the senderName again or just leave it empty for a "neat" look.
                        Text(
                            text = "No public contact details",
                            color = SecondaryText,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)

            // Participants Section
            if (chat.isGroup) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val memberCount = (groupInfo as? Resource.Success<com.example.firstapp.chat.data.api.GroupInfoResponse>)?.data?.member_ids?.size ?: 0
                        Text(
                            text = "$memberCount participants",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search participants",
                            tint = AppAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (chat.isAdmin) {
                        ListItem(
                            headlineContent = { Text("Add members", color = AppAccent) },
                            leadingContent = { 
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(AppAccent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { showAddMemberDialog = true }
                        )
                    }

                    when (groupInfo) {
                        is Resource.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                        }
                        is Resource.Success -> {
                            groupInfo.data?.member_ids?.forEach { memberId ->
                                val memberContact = contacts.find { it.id == memberId }
                                val name = memberContact?.displayName ?: memberId
                                
                                var showMemberMenu by remember { mutableStateOf(false) }

                                ListItem(
                                    headlineContent = { Text(if (memberId == currentUserId) "You" else name) },
                                    supportingContent = { 
                                        val isAdmin = (groupInfo as Resource.Success<com.example.firstapp.chat.data.api.GroupInfoResponse>).data?.admin_ids?.contains(memberId) == true
                                        if (isAdmin) {
                                            Text("Group Admin", color = AppAccent, fontSize = 12.sp)
                                        }
                                    },
                                    leadingContent = {
                                        ChatAvatar(
                                            imageUrl = memberContact?.avatarUrl,
                                            name = name,
                                            size = 40.dp,
                                            backgroundColor = if (memberId == currentUserId) AppAccent else Color.Gray,
                                            textColor = Color.White
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier.clickable {
                                        if (memberId != currentUserId && chat.isAdmin) {
                                            showMemberMenu = true
                                        }
                                    }
                                )

                                if (showMemberMenu) {
                                    DropdownMenu(
                                        expanded = showMemberMenu,
                                        onDismissRequest = { showMemberMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Message $name") },
                                            onClick = { /* Message user */ showMemberMenu = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Make Group Admin") },
                                            onClick = { onPromoteAdmin(memberId); showMemberMenu = false }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Remove $name", color = Color.Red) },
                                            onClick = { onRemoveMember(memberId); showMemberMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                        is Resource.Error -> {
                            // If it's a group, we might have partial info from the chat itself
                            if (!chat.isGroup) {
                                Text("Failed to load profile: ${groupInfo.message}", color = Color.Red)
                            }
                        }
                    }
                }
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)
            }

            // Media, Links, and Docs
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Media, links, and docs",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${sharedMedia.size} >",
                        color = SecondaryText,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onNavigateToSharedMedia(chat.id) }
                    )
                }
                
                if (sharedMedia.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        sharedMedia.take(4).forEach { message ->
                            val url = message.mediaUrls.firstOrNull() ?: ""
                            val type = if (message.type == MessageType.VIDEO) "video" else "image"
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Gray.copy(alpha = 0.1f))
                                    .clickable { onMediaClick(url, type) }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (message.type == MessageType.VIDEO) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "No shared media yet",
                        color = SecondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)

            // Settings
            Column {
                ListItem(
                    headlineContent = { Text("Mute notifications") },
                    leadingContent = { Icon(Icons.Default.NotificationsOff, null, tint = AppAccent) },
                    trailingContent = { Switch(checked = chat.isMuted, onCheckedChange = onMuteToggle, colors = SwitchDefaults.colors(checkedThumbColor = AppAccent)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                if (isChannel) {
                    ListItem(
                        headlineContent = { Text(if (chat.isSubscribed) "Unfollow channel" else "Follow channel") },
                        leadingContent = { Icon(if (chat.isSubscribed) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircleOutline, null, tint = AppAccent) },
                        modifier = Modifier.clickable { onSubscribe(chat.id) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                
                if (isCreator && isChannel) {
                    ListItem(
                        headlineContent = { Text("Admins can post") },
                        leadingContent = { Icon(Icons.Default.PostAdd, null, tint = AppAccent) },
                        trailingContent = { 
                            Switch(
                                checked = chat.adminsCanPost, 
                                onCheckedChange = onToggleAdminPosting,
                                colors = SwitchDefaults.colors(checkedThumbColor = AppAccent)
                            ) 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                
                ListItem(
                    headlineContent = { Text("Custom notifications") },
                    leadingContent = { Icon(Icons.Default.MusicNote, null, tint = AppAccent) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { }
                )
                ListItem(
                    headlineContent = { Text("Media visibility") },
                    leadingContent = { Icon(Icons.Default.Image, null, tint = AppAccent) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { }
                )
                
                ListItem(
                    headlineContent = { Text("Disappearing messages") },
                    supportingContent = { Text("Off") },
                    leadingContent = { Icon(Icons.Default.Timer, null, tint = AppAccent) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { }
                )
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)
            
            // Encryption
            ListItem(
                headlineContent = { Text("Encryption") },
                supportingContent = { Text("Messages and calls are end-to-end encrypted. Tap to verify.") },
                leadingContent = { Icon(Icons.Default.Lock, null, tint = AppAccent) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { }
            )

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)

            // Actions
            if (chat.isGroup) {
                TextButton(
                    onClick = onLeaveGroup,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null)
                        Spacer(Modifier.width(16.dp))
                        Text("Exit group", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                TextButton(
                    onClick = onClearChat,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DeleteSweep, null)
                        Spacer(Modifier.width(16.dp))
                        Text("Clear Chat", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            TextButton(
                onClick = onDeleteChat,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(16.dp))
                    Text(if (chat.isGroup) "Delete group" else "Delete Chat", fontWeight = FontWeight.Bold)
                }
            }
            
            if (!chat.isGroup) {
                TextButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Report, null)
                        Spacer(Modifier.width(16.dp))
                        Text("Report Contact", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(Modifier.height(50.dp))
        }
    }

    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add members") },
            text = {
                val memberIds = (groupInfo as? Resource.Success<com.example.firstapp.chat.data.api.GroupInfoResponse>)?.data?.member_ids ?: emptyList()
                val availableContacts = contacts.filter { contact ->
                    !memberIds.contains(contact.id) && contact.isRegistered
                }
                
                if (availableContacts.isEmpty()) {
                    Text("No more contacts to add.")
                } else {
                    Box(modifier = Modifier.height(300.dp)) {
                        LazyColumn {
                            items(availableContacts) { contact ->
                                ListItem(
                                    headlineContent = { Text(contact.displayName) },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                                .background(Color.Gray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (contact.avatarUrl != null) {
                                                AsyncImage(
                                                    model = contact.avatarUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Text(
                                                    contact.displayName.take(1).uppercase(),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier.clickable { 
                                        onAddMember(contact.id)
                                        showAddMemberDialog = false 
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
