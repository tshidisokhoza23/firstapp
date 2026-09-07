package com.example.firstapp.chat.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.chat.data.api.GroupInfoResponse
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.ui.components.ChatAvatar
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    chat: ChatMessage,
    groupInfo: Resource<GroupInfoResponse>,
    contacts: List<Contact>,
    sharedMedia: List<Message>,
    currentUserId: String?,
    inviteCode: String?,
    onBack: () -> Unit,
    onOpenChat: () -> Unit,
    onMute: (Boolean) -> Unit,
    onAddMember: (String) -> Unit,
    onRemoveMember: (String) -> Unit,
    onPromoteAdmin: (String) -> Unit,
    onMessageMember: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    onGenerateInvite: () -> Unit,
    onSharedMedia: () -> Unit,
    onMediaClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val info = groupInfo.data
    val memberIds = info?.member_ids.orEmpty()
    val adminIds = info?.admin_ids.orEmpty()
    val isAdmin = currentUserId != null && (chat.isAdmin || adminIds.contains(currentUserId))
    val isCreator = currentUserId != null && (info?.created_by == currentUserId || chat.createdBy == currentUserId)
    val groupName = info?.title?.takeIf { it.isNotBlank() }
        ?: info?.name?.takeIf { it.isNotBlank() }
        ?: chat.senderName.ifBlank { "Group" }
    val inviteLink = inviteCode?.takeIf { it.isNotBlank() }?.let { "firstapp://group/invite/$it" }
    var showAddMember by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var memberQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group info", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, inviteLink ?: "Join $groupName on TJ")
                        }
                        context.startActivity(Intent.createChooser(share, "Share group"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Open chat") },
                                onClick = { showMenu = false; onOpenChat() }
                            )
                            if (isAdmin) {
                                DropdownMenuItem(
                                    text = { Text("Add members") },
                                    onClick = { showMenu = false; showAddMember = true }
                                )
                                DropdownMenuItem(
                                    text = { Text("Invite via link") },
                                    onClick = { showMenu = false; onGenerateInvite() }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ChatAvatar(
                        imageUrl = info?.avatar_url ?: chat.avatarUrl,
                        name = groupName,
                        size = 96.dp,
                        fontSize = 36.sp,
                        backgroundColor = chat.avatarColor,
                        textColor = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(groupName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${memberIds.size.coerceAtLeast(1)} participants",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    if (!info?.description.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            info?.description.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), thickness = 8.dp)
                ListItem(
                    headlineContent = { Text("Mute notifications") },
                    leadingContent = { Icon(Icons.Default.NotificationsOff, null, tint = AppAccent) },
                    trailingContent = {
                        Switch(
                            checked = chat.isMuted,
                            onCheckedChange = onMute,
                            colors = SwitchDefaults.colors(checkedThumbColor = AppAccent)
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (isAdmin) {
                    ListItem(
                        headlineContent = { Text("Add members") },
                        supportingContent = { Text("Only admins can add people") },
                        leadingContent = { Icon(Icons.Default.PersonAdd, null, tint = AppAccent) },
                        modifier = Modifier.clickable { showAddMember = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Invite via link") },
                        supportingContent = {
                            Text(inviteLink ?: "Create a link anyone can use to join")
                        },
                        leadingContent = { Icon(Icons.Default.Link, null, tint = AppAccent) },
                        trailingContent = {
                            if (inviteLink != null) {
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(inviteLink))
                                    Toast.makeText(context, "Invite link copied", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        },
                        modifier = Modifier.clickable {
                            if (inviteLink == null) onGenerateInvite()
                            else {
                                clipboard.setText(AnnotatedString(inviteLink))
                                Toast.makeText(context, "Invite link copied", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), thickness = 8.dp)
            }

            item {
                ListItem(
                    headlineContent = { Text("Media, links, and docs") },
                    trailingContent = { Text("${sharedMedia.size}", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.clickable(onClick = onSharedMedia),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (sharedMedia.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sharedMedia.take(4).forEach { message ->
                            val url = message.mediaUrls.firstOrNull().orEmpty()
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onMediaClick(url, "image") }
                            ) {
                                if (url.isNotBlank()) {
                                    AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), thickness = 8.dp)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${memberIds.size} participants",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.Search, contentDescription = null, tint = AppAccent, modifier = Modifier.size(18.dp))
                }
                OutlinedTextField(
                    value = memberQuery,
                    onValueChange = { memberQuery = it },
                    placeholder = { Text("Search members") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
            }

            val visibleMembers = memberIds.filter { id ->
                val contact = contacts.find { it.id == id }
                val name = if (id == currentUserId) "You" else contact?.displayName ?: id
                memberQuery.isBlank() || name.contains(memberQuery, ignoreCase = true)
            }
            items(visibleMembers, key = { it }) { memberId ->
                val contact = contacts.find { it.id == memberId }
                val name = if (memberId == currentUserId) "You" else contact?.displayName ?: memberId
                val memberIsAdmin = adminIds.contains(memberId)
                var showMemberMenu by remember { mutableStateOf(false) }
                ListItem(
                    headlineContent = { Text(name) },
                    supportingContent = {
                        Text(
                            when {
                                info?.created_by == memberId -> "Group creator"
                                memberIsAdmin -> "Group admin"
                                else -> contact?.phoneNumber ?: "Member"
                            },
                            color = if (memberIsAdmin) AppAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    },
                    leadingContent = {
                        ChatAvatar(
                            imageUrl = contact?.avatarUrl,
                            name = name,
                            size = 40.dp,
                            backgroundColor = if (memberId == currentUserId) AppAccent else Color.Gray,
                            textColor = Color.White
                        )
                    },
                    trailingContent = {
                        if (memberIsAdmin) {
                            Icon(Icons.Default.AdminPanelSettings, null, tint = AppAccent, modifier = Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier.clickable {
                        if (memberId != currentUserId) showMemberMenu = true
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                DropdownMenu(expanded = showMemberMenu, onDismissRequest = { showMemberMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Message $name") },
                        onClick = { showMemberMenu = false; onMessageMember(memberId) }
                    )
                    if (isAdmin && memberId != currentUserId) {
                        if (!memberIsAdmin) {
                            DropdownMenuItem(
                                text = { Text("Make group admin") },
                                onClick = { showMemberMenu = false; onPromoteAdmin(memberId) }
                            )
                        }
                        if (!(info?.created_by == memberId && !isCreator)) {
                            DropdownMenuItem(
                                text = { Text("Remove $name", color = Color.Red) },
                                onClick = { showMemberMenu = false; onRemoveMember(memberId) }
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), thickness = 8.dp)
                ListItem(
                    headlineContent = { Text("Exit group", color = Color.Red, fontWeight = FontWeight.Bold) },
                    supportingContent = {
                        Text(
                            if (isCreator) "If you leave and no admin remains, the oldest member becomes admin"
                            else "You will stop receiving messages"
                        )
                    },
                    leadingContent = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Color.Red) },
                    modifier = Modifier.clickable { showLeaveConfirm = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Report group", color = Color.Red) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    if (showAddMember) {
        val available = contacts.filter { it.isRegistered && !memberIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Add members") },
            text = {
                if (available.isEmpty()) {
                    Text("No more contacts to add. They must be registered on TJ.")
                } else {
                    Box(modifier = Modifier.height(320.dp)) {
                        LazyColumn {
                            items(available, key = { it.id }) { contact ->
                                ListItem(
                                    headlineContent = { Text(contact.displayName) },
                                    supportingContent = { Text(contact.phoneNumber) },
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(contact.displayName.take(1).uppercase(), color = Color.White)
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        onAddMember(contact.id)
                                        showAddMember = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddMember = false }) { Text("Cancel") } }
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Exit group?") },
            text = { Text("You will no longer receive messages from $groupName.") },
            confirmButton = {
                TextButton(onClick = { showLeaveConfirm = false; onLeaveGroup() }) { Text("Exit", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancel") } }
        )
    }
}
