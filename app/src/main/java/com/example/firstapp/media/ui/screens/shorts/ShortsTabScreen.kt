package com.example.firstapp.media.ui.screens.shorts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
    val currentUser = com.example.firstapp.LocalCurrentUser.current
    val uiState by mediaViewModel.uiState.collectAsState()
    val shortsPagingItems = uiState.posts.collectAsLazyPagingItems()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var selectedShortId by remember { mutableStateOf(initialShortId) }
    val isUploading by mediaViewModel.isUploading.collectAsState()
    
    var showShortComments by remember { mutableStateOf(false) }
    var showShortMoreMenu by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var activeShortIdForComments by remember { mutableStateOf<String?>(null) }
    var activeShortForMoreMenu by remember { mutableStateOf<Post?>(null) }
    val shortRepliesState by mediaViewModel.shortReplies.collectAsState()

    LaunchedEffect(Unit) {
        mediaViewModel.loadFeed("short")
        userViewModel.fetchCurrentUser()
    }

    BackHandler(enabled = showShortComments || showShortMoreMenu || showUploadDialog || showTopMenu) {
        when {
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

    Box(modifier = Modifier.fillMaxSize().background(AppBackground).nestedScroll(scrollBehavior.nestedScrollConnection)) {
        if (shortsPagingItems.itemCount == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No videos found", color = Color.White, fontSize = 18.sp)
            }
        } else {
            ShortsList(
                adManager = adManager,
                shorts = shortsPagingItems,
                shortsViewModel = mediaViewModel,
                currentUserAvatarUrl = currentUser.avatarUrl,
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
                    navController.navigate(com.example.firstapp.ui.navigation.Screen.UserProfile.createRoute(creatorId))
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
            navigationIcon = {
                if (initialShortId != null) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                        leadingIcon = { Icon(Icons.Default.Settings, null, tint = Color.White) },
                        onClick = { showTopMenu = false }
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            ),
            scrollBehavior = scrollBehavior,
            windowInsets = WindowInsets(0, 0, 0, 0)
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
                    mediaViewModel.createPost(
                        content = content,
                        parentPostId = parentId ?: activeShortIdForComments,
                        rootPostId = activeShortIdForComments,
                        postType = "reply"
                    )
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
