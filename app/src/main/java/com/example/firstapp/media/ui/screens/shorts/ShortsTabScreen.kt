package com.example.firstapp.media.ui.screens.shorts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.firstapp.common.model.User
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.home.ui.screens.DataStateHandler
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.profile.ui.screens.ProfileView
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel
import java.io.File
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsTabScreen(
    navController: NavController,
    shortsViewModel: ShortsViewModel = hiltViewModel(),
    mediaViewModel: MediaViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    onDirectMessagesClick: () -> Unit = {},
    initialShortId: String? = null
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val shortsState by shortsViewModel.shorts.collectAsState()
    
    LaunchedEffect(initialShortId) {
        if (initialShortId != null && initialShortId.length > 0) {
            shortsViewModel.setSelectedShortId(initialShortId)
        }
    }

    val selectedShortId by shortsViewModel.selectedShortId.collectAsState()
    val isUploading by shortsViewModel.isUploading.collectAsState()
    
    var showShortComments by remember { mutableStateOf(false) }
    var showShortMoreMenu by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var activeShortIdForComments by remember { mutableStateOf<String?>(null) }
    var activeShortForMoreMenu by remember { mutableStateOf<ShortMedia?>(null) }
    var selectedProfileUser by remember { mutableStateOf<User?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    val shortRepliesState by shortsViewModel.shortReplies.collectAsState()
    val selectedProfileUserId = selectedProfileUser?.id
    val profilePosts = remember(selectedProfileUserId) {
        if (selectedProfileUserId.isNullOrBlank()) {
            flowOf(PagingData.empty())
        } else {
            mediaViewModel.getUserPosts(selectedProfileUserId)
        }
    }.collectAsLazyPagingItems()

    BackHandler(enabled = showProfile) {
        showProfile = false
        selectedProfileUser = null
    }

    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { /* show upload dialog with this video */ }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        if (showProfile && selectedProfileUser != null) {
            ProfileView(
                userId = selectedProfileUserId,
                showExperienceTab = false,
                onBack = {
                    showProfile = false
                    selectedProfileUser = null
                },
                posts = profilePosts,
                userViewModel = userViewModel,
                mediaViewModel = mediaViewModel,
                onNavigateToSharedMedia = { target ->
                    if (target == "inbox") {
                        navController.navigate(com.example.firstapp.ui.navigation.Screen.MediaMessages.route)
                    }
                },
                onNavigateToMediaViewer = { id, type ->
                    if (type == "chat") {
                        navController.navigate(com.example.firstapp.ui.navigation.Screen.MediaChat.createRoute(id))
                    } else {
                        navController.navigate(com.example.firstapp.ui.navigation.Screen.MediaViewer.createRoute(id, type))
                    }
                },
                onRequireLogin = {
                    navController.navigate(com.example.firstapp.ui.navigation.Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        } else {
            DataStateHandler(
                resource = shortsState,
                onEmpty = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No videos found", color = Color.White, fontSize = 18.sp)
                    }
                },
                onError = { error ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Error loading shorts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(error ?: "Unknown error occurred", color = Color.Gray, fontSize = 14.sp)
                            Button(onClick = { shortsViewModel.fetchShorts() }, modifier = Modifier.padding(top = 16.dp)) {
                                Text("Retry")
                            }
                        }
                    }
                }
            ) { shorts ->
                ShortsList(
                    shorts = shorts,
                    shortsViewModel = shortsViewModel,
                    bottomPadding = 0.dp,
                    initialShortId = selectedShortId,
                    onShortDisplayed = { shortsViewModel.setSelectedShortId(null) },
                    onShowComments = { id ->
                        activeShortIdForComments = id
                        shortsViewModel.fetchShortReplies(id)
                        showShortComments = true
                    },
                    onShowMoreMenu = { short ->
                        activeShortForMoreMenu = short
                        showShortMoreMenu = true
                    },
                    onCreatorClick = { creatorId ->
                        val short = shorts.firstOrNull { it.creatorId == creatorId }
                        selectedProfileUser = User(
                            id = creatorId,
                            username = short?.creatorUsername ?: "",
                            displayName = short?.creatorName ?: "",
                            avatarUrl = short?.creatorAvatarUrl
                        )
                        showProfile = true
                    }
                )
            }

            if (isUploading) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            TopAppBar(
                title = { Text("Shorts", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    val avatarUrl = currentUser?.avatarUrl
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A4B57))
                            .clickable(enabled = !currentUser?.id.isNullOrBlank()) {
                                selectedProfileUser = currentUser
                                showProfile = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showUploadDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Upload", tint = Color.White)
                    }
                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false },
                        modifier = Modifier.background(AppSecondary)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Direct Messages", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.Mail, null, tint = Color.White) },
                            onClick = {
                                showTopMenu = false
                                onDirectMessagesClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings", color = Color.White) },
                            onClick = { showTopMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Help & Feedback", color = Color.White) },
                            onClick = { showTopMenu = false }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )

            if (showUploadDialog) {
                UploadShortDialog(
                    onDismiss = { showUploadDialog = false },
                    onUpload = { file, desc ->
                        shortsViewModel.uploadShort(file, desc, null)
                        showUploadDialog = false
                    }
                )
            }

            // Upload error feedback
            val state by shortsViewModel.shorts.collectAsState()
            val context = LocalContext.current
            if (state is Resource.Error) {
                LaunchedEffect(state) {
                    android.widget.Toast.makeText(context, state.message ?: "Upload failed", android.widget.Toast.LENGTH_LONG).show()
                }
            }

            if (showShortComments && activeShortIdForComments != null) {
                ShortCommentsSheet(
                    shortId = activeShortIdForComments!!,
                    repliesState = shortRepliesState,
                    currentUserAvatarUrl = currentUser?.avatarUrl,
                    onDismiss = {
                        showShortComments = false
                        activeShortIdForComments = null
                        shortsViewModel.clearActiveShortReplies()
                    },
                    onSendReply = { content, parentId ->
                        shortsViewModel.replyToShort(activeShortIdForComments!!, content, parentId)
                    },
                    onLikeReply = { replyId ->
                        shortsViewModel.likeShortReply(replyId)
                    }
                )
            }

            if (showShortMoreMenu && activeShortForMoreMenu != null) {
                ShortMoreMenuSheet(
                    short = activeShortForMoreMenu!!,
                    onDismiss = {
                        showShortMoreMenu = false
                        activeShortForMoreMenu = null
                    },
                    onAction = { action ->
                        if (action == "message") {
                            navController.navigate(com.example.firstapp.ui.navigation.Screen.MediaChat.createRoute(activeShortForMoreMenu!!.creatorId))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun UploadShortDialog(
    onDismiss: () -> Unit,
    onUpload: (File, String) -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var selectedVideoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedVideoUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Short", color = Color.White) },
        text = {
            Column {
                if (selectedVideoUri == null) {
                    Button(
                        onClick = { videoLauncher.launch("video/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Video")
                    }
                } else {
                    Text("Video selected", color = Color.Green)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { videoLauncher.launch("video/*") }) {
                        Text("Change Video")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val uri = selectedVideoUri
                    if (uri != null) {
                        val file = com.example.firstapp.common.util.MediaUtils.getFileFromUri(context, uri)
                        if (file != null) {
                            onUpload(file, description)
                        }
                    }
                },
                enabled = selectedVideoUri != null
            ) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = AppSecondary
    )
}
