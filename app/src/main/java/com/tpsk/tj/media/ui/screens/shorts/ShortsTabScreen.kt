package com.tpsk.tj.media.ui.screens.shorts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.media.ui.screens.SearchTopBar
import kotlin.collections.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsTabScreen(
    navController: NavController,
    adManager: com.tpsk.tj.common.data.ads.AdManager,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    mediaViewModel: MediaViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    onDirectMessagesClick: () -> Unit = {},
    initialShortId: String? = null
) {
    val currentUser = com.tpsk.tj.LocalCurrentUser.current
    val uiState by mediaViewModel.uiState.collectAsState()
    val shortsPagingItems = uiState.posts.collectAsLazyPagingItems()
    val isGlobalPaused by mediaViewModel.isGlobalPlaybackPaused.collectAsState()
    val searchResults by mediaViewModel.searchResults.collectAsState()
    val autoplayEnabled by hiltViewModel<com.tpsk.tj.home.ui.viewmodel.MainViewModel>().autoplayVideos.collectAsState()
    val autoplayGifsEnabled by hiltViewModel<com.tpsk.tj.home.ui.viewmodel.MainViewModel>().autoplayGifs.collectAsState()

    var selectedShortId by remember { mutableStateOf(initialShortId) }
    val isUploading by mediaViewModel.isUploading.collectAsState()
    
    var showShortComments by remember { mutableStateOf(false) }
    var showShortMoreMenu by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var activeShortIdForComments by remember { mutableStateOf<String?>(null) }
    var activeShortForMoreMenu by remember { mutableStateOf<Post?>(null) }
    val shortRepliesState by mediaViewModel.shortReplies.collectAsState()
    val preloadedShortAds by mediaViewModel.preloadedShortAds.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        android.util.Log.d("ShortsTabScreen", "Launched: initialShortId=$initialShortId")
        mediaViewModel.loadFeed("short")
        userViewModel.fetchCurrentUser()
    }

    LaunchedEffect(isSearchActive) {
        mediaViewModel.setGlobalPlaybackPaused(isSearchActive)
    }

    BackHandler(enabled = showShortComments || showShortMoreMenu || showUploadDialog || isSearchActive) {
        when {
            isSearchActive -> isSearchActive = false
            showShortComments -> showShortComments = false
            showShortMoreMenu -> {
                showShortMoreMenu = false
                activeShortForMoreMenu = null
            }
            showUploadDialog -> showUploadDialog = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (shortsPagingItems.itemCount == 0) {
                val loadState = shortsPagingItems.loadState
                if (loadState.refresh is androidx.paging.LoadState.Loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VideoLibrary, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No shorts found", color = Color.White.copy(alpha = 0.9f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Be the first to post!", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { showUploadDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Create Short", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            if (loadState.refresh is androidx.paging.LoadState.Error) {
                                Spacer(Modifier.height(8.dp))
                                Text((loadState.refresh as androidx.paging.LoadState.Error).error.localizedMessage ?: "Network error", color = Color.Red.copy(alpha = 0.5f), fontSize = 12.sp)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { shortsPagingItems.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            } else {
                ShortsList(
                    adManager = adManager,
                    shorts = shortsPagingItems,
                    shortsViewModel = mediaViewModel,
                    preloadedAds = preloadedShortAds,
                    currentUserAvatarUrl = currentUser?.avatarUrl,
                    ownUserId = currentUser?.id.orEmpty(),
                    bottomPadding = bottomPadding,
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
                        navController.navigate(com.tpsk.tj.ui.navigation.Screen.UserProfile.createRoute(creatorId, tab = "Shorts"))
                    },
                    isGlobalPaused = isGlobalPaused
                )
            }
        }

        ShortsTopOverlay(
            onBackClick = { navController.popBackStack() },
            onSearchClick = { isSearchActive = true },
            showBack = initialShortId != null
        )

        if (isSearchActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    com.tpsk.tj.media.ui.screens.SearchTopBar(
                        query = searchQuery,
                        onQueryChange = { 
                            searchQuery = it 
                            mediaViewModel.search(it, true)
                        },
                        onClose = { 
                            isSearchActive = false
                            searchQuery = ""
                        }
                    )
                    
                    com.tpsk.tj.media.ui.screens.SearchResultsList(
                        results = searchResults,
                        autoplayEnabled = autoplayEnabled,
                        autoplayGifsEnabled = autoplayGifsEnabled,
                        onPostClick = { post ->
                            if (post.type == com.tpsk.tj.media.model.PostType.SHORT) {
                                isSearchActive = false
                                selectedShortId = post.id
                            } else {
                                navController.navigate(com.tpsk.tj.ui.navigation.Screen.PostDetail.createRoute(post.id))
                            }
                        },
                        onProfileClick = { creatorId ->
                            navController.navigate(com.tpsk.tj.ui.navigation.Screen.UserProfile.createRoute(creatorId))
                        }
                    )
                }
            }
        }

        if (isUploading) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (showUploadDialog) {
            UploadShortDialog(
                onDismiss = { showUploadDialog = false },
                onUpload = { uri, desc ->
                    mediaViewModel.createPost(desc, java.util.Arrays.asList(uri.toString()), postType = "short", mediaType = "video")
                    showUploadDialog = false
                }
            )
        }

        val activeId = activeShortIdForComments
        AnimatedVisibility(
            visible = showShortComments && activeId != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            if (activeId != null) {
                ShortCommentsOverlay(
                    shortId = activeId,
                    repliesState = shortRepliesState,
                    currentUserAvatarUrl = currentUser?.avatarUrl,
                    bottomPadding = bottomPadding,
                    onDismiss = { showShortComments = false },
                    onSendReply = { content, parentId ->
                        mediaViewModel.createPost(content, parentPostId = parentId ?: activeId, rootPostId = activeId, postType = "reply")
                    },
                    onLikeReply = { reply ->
                        mediaViewModel.likePost(reply.id, reply.likes, reply.isLiked)
                    }
                )
            }
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
                        navController.navigate(com.tpsk.tj.ui.navigation.Screen.MediaChat.createRoute(activeShortForMoreMenu!!.authorId))
                    } else if (action == "add_short") {
                        showUploadDialog = true
                    }
                }
            )
        }
    }
}

@Composable
fun ShortsTopOverlay(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    showBack: Boolean = true
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun UploadShortDialog(
    onDismiss: () -> Unit,
    onUpload: (android.net.Uri, String) -> Unit
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
                    Button(onClick = { videoLauncher.launch("video/*") }, modifier = Modifier.fillMaxWidth()) {
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
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val uri = selectedVideoUri
                    if (uri != null) {
                        onUpload(uri, description)
                    }
                },
                enabled = selectedVideoUri != null
            ) { Text("Upload") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = com.tpsk.tj.common.ui.theme.AppSecondary
    )
}
