package com.tpsk.tj.groups.ui

import com.tpsk.tj.common.util.sanitizeDisplayName

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.tpsk.tj.chat.data.api.GroupInfoResponse
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.chat.ui.components.ChatAvatar
import com.tpsk.tj.common.ui.theme.AppAccent
import com.tpsk.tj.common.util.Resource

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
    onDemoteAdmin: (String) -> Unit = {},
    onMessageMember: (String) -> Unit,
    onLeaveGroup: () -> Unit,
    onGenerateInvite: () -> Unit,
    onRename: (String) -> Unit = {},
    onChangeAvatar: (Uri) -> Unit = {},
    onSharedMedia: () -> Unit,
    onMediaClick: (String, String) -> Unit,
    onReport: (String) -> Unit = {},
    onSaveDescription: (String) -> Unit = {}
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
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember(groupName) { mutableStateOf(groupName) }
    var showReport by remember { mutableStateOf(false) }
    var showDescription by remember { mutableStateOf(false) }
    var descriptionText by remember(info?.description) { mutableStateOf(info?.description.orEmpty()) }
    val visibleMedia = if (chat.mediaVisibility) sharedMedia else emptyList()
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onChangeAvatar(uri)
    }

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
                    Box {
                        ChatAvatar(
                            imageUrl = info?.avatar_url ?: chat.avatarUrl,
                            name = groupName,
                            size = 96.dp,
                            fontSize = 36.sp,
                            backgroundColor = chat.avatarColor,
                            textColor = Color.White
                        )
                        if (isAdmin) {
                            IconButton(
                                onClick = { avatarPicker.launch("image/*") },
                                modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(AppAccent)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Change group icon", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        groupName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = isAdmin) { renameText = groupName; showRename = true }
                    )
                    if (isAdmin) {
                        Text("Tap the name to rename", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
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
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onOpenChat,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Open group chat", fontWeight = FontWeight.Bold)
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
                    trailingContent = { Text("${visibleMedia.size}", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.clickable(enabled = chat.mediaVisibility, onClick = onSharedMedia),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (visibleMedia.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleMedia.take(4).forEach { message ->
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
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search members",
                        tint = AppAccent,
                        modifier = Modifier.size(18.dp)
                    )
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
                val name = if (id == currentUserId) "You" else sanitizeDisplayName(contact?.displayName, id, contact?.phoneNumber)
                memberQuery.isBlank() || name.contains(memberQuery, ignoreCase = true)
            }
            items(visibleMembers, key = { it }) { memberId ->
                val contact = contacts.find { it.id == memberId }
                val name = if (memberId == currentUserId) "You" else sanitizeDisplayName(contact?.displayName, memberId, contact?.phoneNumber)
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
                        } else if (info?.created_by != memberId) {
                            DropdownMenuItem(
                                text = { Text("Dismiss as admin") },
                                onClick = { showMemberMenu = false; onDemoteAdmin(memberId) }
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
                if (isAdmin) {
                    ListItem(
                        headlineContent = { Text("Add description") },
                        leadingContent = { Icon(Icons.Default.Edit, null, tint = AppAccent) },
                        modifier = Modifier.clickable { showDescription = true },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                ListItem(
                    headlineContent = { Text("Report group", color = Color.Red) },
                    leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                    modifier = Modifier.clickable { showReport = true },
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
    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Group name") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(80) },
                    singleLine = true,
                    placeholder = { Text("Group subject") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val next = renameText.trim()
                    if (next.isNotEmpty()) onRename(next)
                    showRename = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } }
        )
    }

    if (showDescription) {
        AlertDialog(
            onDismissRequest = { showDescription = false },
            title = { Text("Add description") },
            text = {
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it.take(500) },
                    placeholder = { Text("Group description") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val next = descriptionText.trim()
                    if (next.isNotEmpty()) onSaveDescription(next)
                    showDescription = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDescription = false }) { Text("Cancel") } }
        )
    }
    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("Report group") },
            text = {
                Column {
                    listOf("Spam", "Harassment", "Inappropriate content").forEach { reason ->
                        TextButton(onClick = { showReport = false; onReport(reason) }) { Text(reason) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showReport = false }) { Text("Cancel") } }
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
