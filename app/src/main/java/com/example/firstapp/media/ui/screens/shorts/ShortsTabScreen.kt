package com.example.firstapp.media.ui.screens.shorts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import com.example.firstapp.common.ui.components.DataStateHandler
import com.example.firstapp.media.model.Post
import com.example.firstapp.profile.ui.screens.ProfileView
import java.io.File
import kotlinx.coroutines.flow.flowOf
import kotlin.collections.*
import kotlin.ranges.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsTabScreen(
    navController: NavController,
    adManager: com.example.firstapp.common.data.ads.AdManager,
    mediaViewModel: MediaViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    onDirectMessagesClick: () -> Unit = {},
    initialShortId: String? = null
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val uiState by mediaViewModel.uiState.collectAsState()
    val shortsPagingItems = uiState.posts.collectAsLazyPagingItems()

    var selectedShortId by remember { mutableStateOf(initialShortId) }
    val isUploading by mediaViewModel.isUploading.collectAsState()
    
    var showShortComments by remember { mutableStateOf(false) }
    var showShortMoreMenu by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var activeShortIdForComments by remember { mutableStateOf<String?>(null) }
    var activeShortForMoreMenu by remember { mutableStateOf<Post?>(null) }
    var selectedProfileUser by remember { mutableStateOf<User?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    val shortRepliesState by mediaViewModel.shortReplies.collectAsState()

    LaunchedEffect(Unit) {
        mediaViewModel.loadFeed("short")
        if (currentUser == null) {
            userViewModel.fetchCurrentUser()
        }
    }

    val selectedProfileUserId = selectedProfileUser?.id
    val profilePosts = remember(selectedProfileUserId) {
        if (selectedProfileUserId == null || selectedProfileUserId.length == 0) {
            flowOf(PagingData.empty())
        } else {
            mediaViewModel.getUserPosts(selectedProfileUserId, "standard")
        }
    }.collectAsLazyPagingItems()

    val profileShorts = remember(selectedProfileUserId) {
        if (selectedProfileUserId == null || selectedProfileUserId.length == 0) {
            flowOf(PagingData.empty())
        } else {
            mediaViewModel.getUserPosts(selectedProfileUserId, "short")
        }
    }.collectAsLazyPagingItems()

    val profileMedia = remember(selectedProfileUserId) {
        if (selectedProfileUserId == null || selectedProfileUserId.length == 0) {
            flowOf(PagingData.empty())
        } else {
            mediaViewModel.getUserPosts(selectedProfileUserId, "media")
        }
    }.collectAsLazyPagingItems()

    val profileReplies = remember(selectedProfileUserId) {
        if (selectedProfileUserId == null || selectedProfileUserId.length == 0) {
            flowOf(PagingData.empty())
        } else {
            mediaViewModel.getUserPosts(selectedProfileUserId, "reply")
        }
    }.collectAsLazyPagingItems()

    BackHandler(enabled = showProfile || showShortComments || showShortMoreMenu || showUploadDialog || showTopMenu) {
        when {
            showProfile -> {
                showProfile = false
                selectedProfileUser = null
            }
            showShortComments -> {
                showShortComments = false
                activeShortIdForComments = null
            }
            showShortMoreMenu -> {
                showShortMoreMenu = false
                activeShortForMoreMenu = null
            }
            showUploadDialog -> showUploadDialog = false
            showTopMenu -> showTopMenu = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        if (showProfile && selectedProfileUser != null) {
            ProfileView(
                userId = selectedProfileUserId!!,
                showExperienceTab = false,
                adManager = adManager,
                onBack = {
                    showProfile = false
                    selectedProfileUser = null
                },
                posts = profilePosts,
                shorts = profileShorts,
                mediaItems = profileMedia,
                replies = profileReplies,
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
                },
                onShortClick = { short ->
                    showProfile = false
                    selectedProfileUser = null
                    selectedShortId = short.id
                }
            )
        } else {
            if (shortsPagingItems.itemCount == 0) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No videos found", color = Color.White, fontSize = 18.sp)
                }
            } else {
                ShortsList(
                    adManager = adManager,
                    shorts = shortsPagingItems,
                    shortsViewModel = mediaViewModel,
                    currentUserAvatarUrl = currentUser?.avatarUrl,
                    bottomPadding = 0.dp,
                    initialShortId = selectedShortId,
                    onShortDisplayed = { selectedShortId = null },
                    onShowComments = { id ->
                        activeShortIdForComments = id
                        mediaViewModel.fetchShortReplies(id)
                        showShortComments = true
                    },
                    onShowMoreMenu = { short ->
                        activeShortForMoreMenu = short
                        showShortMoreMenu = true
                    },
                        onCreatorClick = { creatorId ->
                            var short: Post? = null
                            val count = shortsPagingItems.itemCount
                            var i = 0
                            while (i < count) {
                                val s = shortsPagingItems[i]
                                if (s?.authorId == creatorId) {
                                    short = s
                                    break
                                }
                                i++
                            }
                            selectedProfileUser = User(
                                id = creatorId,
                                username = short?.authorHandle ?: "",
                                email = "",
                                displayName = short?.authorName ?: "",
                                phoneNumber = "",
                                avatarUrl = short?.authorAvatarUrl
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
                title = { },
                navigationIcon = { },
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
                        mediaViewModel.createPost(desc, java.util.Arrays.asList(file.absolutePath), postType = "short", mediaType = "video")
                        showUploadDialog = false
                    }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showShortComments && activeShortIdForComments != null,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it })
            ) {
                ShortCommentsOverlay(
                    shortId = activeShortIdForComments!!,
                    repliesState = shortRepliesState,
                    currentUserAvatarUrl = currentUser?.avatarUrl,
                    onDismiss = {
                        showShortComments = false
                        activeShortIdForComments = null
                    },
                    onSendReply = { content, parentId ->
                        mediaViewModel.createPostReply(activeShortIdForComments!!, content, parentId)
                    },
                    onLikeReply = { reply ->
                        mediaViewModel.likePost(reply.id, reply.likes, reply.isLiked)
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
                            navController.navigate(com.example.firstapp.ui.navigation.Screen.MediaChat.createRoute(activeShortForMoreMenu!!.authorId))
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
