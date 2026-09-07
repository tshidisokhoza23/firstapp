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
    contactBio: String? = null,
    contactUsername: String? = null,
    isBlocked: Boolean = false,
    onClearChat: () -> Unit = {},
    onDeleteChat: () -> Unit = {},
    onMuteToggle: (Boolean) -> Unit = {},
    onPinToggle: (Boolean) -> Unit = {},
    onAddMember: (String) -> Unit = {},
    onRemoveMember: (String) -> Unit = {},
    onPromoteAdmin: (String) -> Unit = {},
    onLeaveGroup: () -> Unit = {},
    onToggleAdminPosting: (Boolean) -> Unit = {},
    onSubscribe: (String) -> Unit = {},
    onAvatarSelected: (android.net.Uri) -> Unit = {},
    onBlock: () -> Unit = {},
    onUnblock: () -> Unit = {},
    onReport: (String) -> Unit = {},
    onDisappearingChange: (Int) -> Unit = {},
    onMediaVisibilityChange: (Boolean) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showEncryptionInfo by remember { mutableStateOf(false) }
    val currentUser = com.example.firstapp.LocalCurrentUser.current
    val currentUserId = currentUser.id
    val isChannel = chat.type == ConversationType.CHANNEL
    val isGroup = chat.type == ConversationType.GROUP || chat.isGroup
    val isDirect = !isChannel && !isGroup
    val isCreator = chat.createdBy != null && chat.createdBy == currentUserId
    
    val canEditAvatar = remember(chat.id, chat.isAdmin, currentUserId, isDirect) {
        !isDirect && (chat.id == currentUserId || chat.isAdmin)
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
                    var showMore by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMore = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                            if (isDirect) {
                                DropdownMenuItem(
                                    text = { Text(if (chat.isPinned) "Unpin chat" else "Pin chat") },
                                    onClick = { onPinToggle(!chat.isPinned); showMore = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isBlocked) "Unblock" else "Block") },
                                    onClick = {
                                        showMore = false
                                        if (isBlocked) onUnblock() else showBlockDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report") },
                                    onClick = { showMore = false; showReportDialog = true }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Share contact") },
                                onClick = {
                                    showMore = false
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, chat.senderName + (chat.phoneNumber?.let { "\n$it" } ?: ""))
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share contact"))
                                }
                            )
                        }
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
                        if (!chat.phoneNumber.isNullOrBlank()) {
                            Text(
                                text = chat.phoneNumber,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                if (isChannel) {
                    Text("Channel info", color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "This is a broadcast channel for ${chat.senderName}.",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, null, tint = SecondaryText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "${chat.followers} followers", color = SecondaryText, fontSize = 14.sp)
                    }
                } else if (isGroup) {
                    Text("Group info", color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "Group conversation",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Text("Contact info", color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!contactBio.isNullOrBlank()) {
                        Text(
                            text = contactBio,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    val presenceLabel = when {
                        isBlocked -> "Blocked"
                        chat.isOnline -> "online"
                        chat.lastSeen > 0L -> formatDirectLastSeen(chat.lastSeen)
                        else -> null
                    }
                    if (presenceLabel != null) {
                        Text(
                            text = presenceLabel,
                            color = SecondaryText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    val phone = chat.phoneNumber?.takeIf { it.isNotBlank() }
                    val username = contactUsername?.takeIf { it.isNotBlank() }
                    val isUuid = remember(chat.id) {
                        chat.id.lowercase().matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
                    }
                    if (phone != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.Phone, null, tint = AppAccent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(text = phone, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                                Text(text = "Mobile", color = SecondaryText, fontSize = 12.sp)
                            }
                        }
                    }
                    if (!username.isNullOrBlank() && !username.equals(chat.senderName, ignoreCase = true)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(Icons.Default.AlternateEmail, null, tint = AppAccent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(text = username, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                                Text(text = "Username", color = SecondaryText, fontSize = 12.sp)
                            }
                        }
                    }
                    if (phone == null && username.isNullOrBlank()) {
                        if (!isUuid) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.AlternateEmail, null, tint = AppAccent, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(text = chat.id, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                                    Text(text = "Username", color = SecondaryText, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Text(
                                text = "No public contact details",
                                color = SecondaryText,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)

            // Participants Section
            if (isGroup) {
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
                
                if (isDirect) {
                    ListItem(
                        headlineContent = { Text(if (chat.isPinned) "Unpin chat" else "Pin chat") },
                        leadingContent = { Icon(Icons.Default.PushPin, null, tint = AppAccent) },
                        modifier = Modifier.clickable { onPinToggle(!chat.isPinned) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }

                ListItem(
                    headlineContent = { Text("Media visibility") },
                    supportingContent = { Text(if (chat.mediaVisibility) "Shown in gallery" else "Hidden from gallery") },
                    leadingContent = { Icon(Icons.Default.Image, null, tint = AppAccent) },
                    trailingContent = {
                        Switch(
                            checked = chat.mediaVisibility,
                            onCheckedChange = onMediaVisibilityChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = AppAccent)
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                
                ListItem(
                    headlineContent = { Text("Disappearing messages") },
                    supportingContent = { Text(disappearingLabel(chat.disappearingMessagesDuration)) },
                    leadingContent = { Icon(Icons.Default.Timer, null, tint = AppAccent) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { showDisappearingDialog = true }
                )
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)
            
            // Encryption
            ListItem(
                headlineContent = { Text("Encryption") },
                supportingContent = { Text("Messages and calls are end-to-end encrypted. Tap for more info.") },
                leadingContent = { Icon(Icons.Default.Lock, null, tint = AppAccent) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.clickable { showEncryptionInfo = true }
            )

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f), thickness = 8.dp)

            // Actions
            if (isGroup) {
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
                    Text(if (isGroup) "Delete group" else "Delete Chat", fontWeight = FontWeight.Bold)
                }
            }
            
            if (isDirect) {
                TextButton(
                    onClick = { if (isBlocked) onUnblock() else showBlockDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Block, null)
                        Spacer(Modifier.width(16.dp))
                        Text(if (isBlocked) "Unblock" else "Block", fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(
                    onClick = { showReportDialog = true },
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

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block ${chat.senderName}?") },
            text = { Text("They won't be able to message or call you. Existing messages stay on your device until you delete the chat.") },
            confirmButton = {
                TextButton(onClick = { showBlockDialog = false; onBlock() }) { Text("Block", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showReportDialog) {
        val reasons = listOf("Spam", "Harassment", "Hate speech", "Scam or fraud", "Inappropriate content", "Other")
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report ${chat.senderName}") },
            text = {
                Column {
                    Text("This report is sent to TJ. The contact is not notified.", color = SecondaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    reasons.forEach { reason ->
                        TextButton(
                            onClick = {
                                showReportDialog = false
                                onReport(reason)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(reason, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDisappearingDialog) {
        val options = listOf(
            0 to "Off",
            24 * 60 * 60 to "24 hours",
            7 * 24 * 60 * 60 to "7 days",
            90 * 24 * 60 * 60 to "90 days"
        )
        AlertDialog(
            onDismissRequest = { showDisappearingDialog = false },
            title = { Text("Disappearing messages") },
            text = {
                Column {
                    Text("New messages in this chat disappear after the selected duration on this device.", color = SecondaryText, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    options.forEach { (seconds, label) ->
                        val selected = chat.disappearingMessagesDuration == seconds
                        TextButton(
                            onClick = {
                                showDisappearingDialog = false
                                onDisappearingChange(seconds)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (selected) "• $label" else label,
                                modifier = Modifier.fillMaxWidth(),
                                color = if (selected) AppAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDisappearingDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showEncryptionInfo) {
        AlertDialog(
            onDismissRequest = { showEncryptionInfo = false },
            title = { Text("Encryption") },
            text = { Text("Messages and calls in this chat use end-to-end encryption when both people have keys set. TJ cannot read the contents of those messages.") },
            confirmButton = {
                TextButton(onClick = { showEncryptionInfo = false }) { Text("OK") }
            }
        )
    }
}

private fun disappearingLabel(durationSeconds: Int): String = when (durationSeconds) {
    0 -> "Off"
    24 * 60 * 60 -> "24 hours"
    7 * 24 * 60 * 60 -> "7 days"
    90 * 24 * 60 * 60 -> "90 days"
    else -> if (durationSeconds > 0) "${durationSeconds / 3600} hours" else "Off"
}

fun formatDirectLastSeen(lastSeenMillis: Long): String {
    if (lastSeenMillis <= 0L) return "offline"
    val now = java.util.Calendar.getInstance()
    val seen = java.util.Calendar.getInstance().apply { timeInMillis = lastSeenMillis }
    val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(lastSeenMillis))
    return when {
        now.get(java.util.Calendar.YEAR) == seen.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == seen.get(java.util.Calendar.DAY_OF_YEAR) ->
            "last seen today at $time"
        now.get(java.util.Calendar.YEAR) == seen.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) - seen.get(java.util.Calendar.DAY_OF_YEAR) == 1 ->
            "last seen yesterday at $time"
        else -> "last seen ${java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(lastSeenMillis))} at $time"
    }
}
