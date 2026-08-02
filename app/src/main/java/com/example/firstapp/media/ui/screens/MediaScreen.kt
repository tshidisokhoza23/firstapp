@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.firstapp.media.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.firstapp.common.model.User
import java.io.File
import java.io.FileOutputStream
import kotlin.text.*
import com.example.firstapp.media.model.*
import com.example.firstapp.music.model.Song
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.formatTwitterTime
import com.example.firstapp.media.ui.components.*
import com.example.firstapp.chat.data.CallType
import kotlin.collections.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.IosShare
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import androidx.paging.LoadState
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.Flow
import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.common.ui.components.DynamicAdCard
import com.example.firstapp.common.data.ads.AdMobManagerImpl // Temporary for non-injected usage if needed
import com.example.firstapp.ui.navigation.Screen
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.*
import kotlin.collections.*
import com.example.firstapp.profile.ui.screens.ProfileView
import com.example.firstapp.common.ui.components.PostItemShimmer
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.flow.flowOf

@Composable
fun MediaScreen(
    adManager: AdManager, // REQUIREMENT: Use Injected Manager
    posts: LazyPagingItems<Post>,
    selectedPost: Post?,
    openOwnProfileRequest: Int = 0,
    closeOwnProfileRequest: Int = 0,
    ownProfileId: String = "",
    ownProfileName: String = "You",
    ownProfileHandle: String = "you",
    ownProfileAvatarUrl: String? = null,
    ownProfileAvatarColor: Color = Color(0xFF3A4B57),
    onPostSelect: (Post?) -> Unit,
    onCreatePost: (content: String, mediaUrls: List<String>, tags: List<String>, postType: String?, pollOptions: List<String>?, location: String?, parentPostId: String?, rootPostId: String?) -> Unit,
    onLikePost: (postId: String, likes: Long, isLiked: Boolean) -> Unit,
    onRepostPost: (postId: String) -> Unit,
    onBookmarkPost: (postId: String) -> Unit,
    onViewPost: (postId: String) -> Unit = {},
    onProfileViewChange: (Boolean) -> Unit = {},
    onGetUserPosts: (String, String) -> Flow<PagingData<Post>> = { _, _ -> kotlinx.coroutines.flow.flowOf() },
    userViewModel: UserViewModel,
    mediaViewModel: MediaViewModel,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onVotePoll: (postId: String, optionId: String) -> Unit = { _, _ -> },
    onFollowUser: (userId: String) -> Unit = {},
    onCallClick: (userId: String, CallType) -> Unit = { _, _ -> },
    onDeletePost: (postId: String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onCreatePostClick: Int = 0,
    onRefresh: () -> Unit = {},
    refreshTrigger: Int = 0,
    onDirectMessagesClick: () -> Unit = {},
    onAdRevenueLogged: (String, Double, String) -> Unit = { _, _, _ -> },
    onShortClick: (Post) -> Unit = {},
    onMediaClick: (String, String, Long) -> Unit = { _, _, _ -> },
    onShareClick: (Post) -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
    var lastRefreshTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(refreshTrigger) {
        val now = System.currentTimeMillis()
        if (refreshTrigger > 0 && now - lastRefreshTime > 1000) {
            lastRefreshTime = now
            posts.refresh()
        }
    }

    var showCreatePost by remember { mutableStateOf(false) }
    var replyingToPost by remember { mutableStateOf<Post?>(null) }
    var replyingToReply by remember { mutableStateOf<Post?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var showRepostOptions by remember { mutableStateOf<Post?>(null) }
    var showTopMoreSheet by remember { mutableStateOf(false) }

    LaunchedEffect(openOwnProfileRequest) {
        if (openOwnProfileRequest > 0) {
            onProfileClick(ownProfileId)
        }
    }

    LaunchedEffect(onCreatePostClick) {
        if (onCreatePostClick > 0) {
            showCreatePost = true
        }
    }

    LaunchedEffect(closeOwnProfileRequest) {
        if (closeOwnProfileRequest > 0) {
            onProfileViewChange(false)
        }
    }

    val preloadedAds by mediaViewModel.preloadedAds.collectAsState()
    val adsLoaded by mediaViewModel.adsLoaded.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        androidx.activity.compose.BackHandler(
            enabled = showCreatePost || replyingToPost != null || isSearchActive || showRepostOptions != null
        ) {
            when {
                isSearchActive -> {
                    isSearchActive = false
                    searchQuery = ""
                }
                showCreatePost -> showCreatePost = false
                replyingToPost != null -> {
                    replyingToPost = null
                    replyingToReply = null
                }
                showRepostOptions != null -> showRepostOptions = null
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier,
                topBar = {
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                        if (isSearchActive) {
                            SearchTopBar(
                                query = searchQuery,
                                onQueryChange = { searchQuery = it },
                                onClose = { 
                                    isSearchActive = false
                                    searchQuery = ""
                                }
                            )
                        } else {
                            TopAppBar(
                                title = {
                                    Text("Media", fontWeight = FontWeight.Bold)
                                },
                                actions = {
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search")
                                    }
                                    IconButton(onClick = { showTopMoreSheet = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    MainFeedSection(
                        adManager = adManager,
                        posts = posts,
                        preloadedAds = preloadedAds,
                        adsLoaded = adsLoaded,
                        ownProfileId = ownProfileId,
                        ownProfileHandle = ownProfileHandle,
                        onPostClick = onPostSelect,
                        onLike = onLikePost,
                        onRepostClick = { showRepostOptions = it },
                        onBookmark = onBookmarkPost,
                        onDeletePost = onDeletePost,
                        onReplyClick = { p, r -> 
                            replyingToPost = p
                            replyingToReply = r
                        },
                        onProfileClick = { id ->
                            onProfileClick(id)
                        },
                        onPlaySong = onPlaySong,
                        onVotePoll = onVotePoll,
                        onRefresh = onRefresh,
                        onShortClick = { short ->
                            onShortClick(short)
                        },
                        onMediaClick = onMediaClick,
                        onFollowUser = onFollowUser
                    )
                }
            }
        }

        if (showTopMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTopMoreSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    ListItem(
                        headlineContent = { Text("View Profile", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { 
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ownProfileAvatarColor)
                            ) {
                                AsyncImage(
                                    model = com.example.firstapp.common.util.MediaUtils.formatUrl(ownProfileAvatarUrl),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = androidx.compose.ui.graphics.painter.ColorPainter(ownProfileAvatarColor),
                                    fallback = androidx.compose.ui.graphics.painter.ColorPainter(ownProfileAvatarColor)
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            showTopMoreSheet = false
                            onProfileClick(ownProfileId)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Create Post", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.clickable {
                            showTopMoreSheet = false
                            showCreatePost = true
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Direct Messages", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Mail, null, tint = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.clickable {
                            showTopMoreSheet = false
                            onDirectMessagesClick()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.clickable {
                            showTopMoreSheet = false
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        if (showRepostOptions != null) {
            val targetPost = showRepostOptions!!
            ModalBottomSheet(
                onDismissRequest = { showRepostOptions = null },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    ListItem(
                        headlineContent = { Text(if (targetPost.isReposted) "Undo Repost" else "Repost", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Repeat, null, tint = if (targetPost.isReposted) Color(0xFF00BA7C) else MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.clickable {
                            onRepostPost(targetPost.id)
                            showRepostOptions = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Quote Post", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.clickable {
                            showRepostOptions = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        if (showCreatePost) {
            PostComposerDialog(
                avatarUrl = ownProfileAvatarUrl,
                avatarColor = ownProfileAvatarColor,
                onDismiss = { showCreatePost = false },
                onSubmit = { content, media, tags, type, poll, loc ->
                    onCreatePost(content, media, tags, type, poll, loc, null, null)
                    showCreatePost = false
                }
            )
        }

        if (replyingToPost != null) {
            val rootId = replyingToPost!!.rootPostId ?: replyingToPost!!.id
            PostComposerDialog(
                replyingTo = replyingToReply ?: replyingToPost,
                avatarUrl = ownProfileAvatarUrl,
                avatarColor = ownProfileAvatarColor,
                onDismiss = { 
                    replyingToPost = null
                    replyingToReply = null
                },
                onSubmit = { content, media, tags, type, poll, loc ->
                    onCreatePost(content, media, tags, type, poll, loc, replyingToReply?.id ?: replyingToPost?.id, rootId)
                    replyingToPost = null
                    replyingToReply = null
                }
            )
        }
    }
}

@Composable
fun MainFeedSection(
    adManager: AdManager,
    posts: LazyPagingItems<Post>,
    preloadedAds: List<Any>,
    adsLoaded: Boolean,
    ownProfileId: String,
    ownProfileHandle: String = "",
    onPostClick: (Post) -> Unit,
    onLike: (String, Long, Boolean) -> Unit,
    onRepostClick: (Post) -> Unit,
    onBookmark: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onReplyClick: (Post, Post?) -> Unit,
    onProfileClick: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onAdRevenueLogged: (String, Double, String) -> Unit = { _, _, _ -> },
    onShortClick: (Post) -> Unit = {},
    onMediaClick: (String, String, Long) -> Unit = { _, _, _ -> },
    onShareClick: (Post) -> Unit = {},
    onFollowUser: (String) -> Unit = {}
) {
    when {
        posts.loadState.refresh is LoadState.Loading && posts.itemCount == 0 -> {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(5) { PostItemShimmer() }
            }
            return
        }
        posts.loadState.refresh is LoadState.Error && posts.itemCount == 0 -> {
            val error = (posts.loadState.refresh as LoadState.Error).error
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Could not load media feed", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error.localizedMessage ?: "Connection error", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { posts.retry(); onRefresh() }) { Text("Retry") }
            }
            return
        }
        posts.itemCount == 0 && posts.loadState.refresh is LoadState.NotLoading -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "No posts yet", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            return
        }
    }

    PullToRefreshBox(
        isRefreshing = posts.loadState.refresh is LoadState.Loading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        val listState = rememberLazyListState()
        val activePostId by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.size == 0) return@derivedStateOf null
                
                // Find item closest to the middle of the screen
                val center = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                var minDistance = Int.MAX_VALUE
                var closestIndex = -1
                
                for (item in visibleItems) {
                    val itemCenter = item.offset + (item.size / 2)
                    val distance = Math.abs(itemCenter - center)
                    if (distance < minDistance) {
                        minDistance = distance
                        closestIndex = item.index
                    }
                }
                
                if (closestIndex != -1 && closestIndex < posts.itemCount) {
                    posts.peek(closestIndex)?.id
                } else null
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            items(
                count = posts.itemCount,
                key = posts.itemKey { it?.id ?: "loading_${it.hashCode()}" },
                contentType = posts.itemContentType { "post" }
            ) { index ->
                val post = posts[index]
                if (post != null) {
                        PostItem(
                            adManager = adManager,
                            post = post,
                            isOwnPost = post.authorId == ownProfileId || (ownProfileHandle.isNotEmpty() && post.authorHandle == ownProfileHandle),
                            showThreadLine = post.hasThread,
                            isLastInThread = true,
                            activePostId = activePostId,
                            onClick = { onPostClick(post) },
                            onLike = { id, likes, liked -> onLike(id, likes, liked) },
                            onRepostClick = { p -> onRepostClick(p) },
                            onBookmark = { id -> onBookmark(id) },
                            onDelete = { id -> onDeletePost(id) },
                            onReplyClick = { p, r -> onReplyClick(p, r) },
                            onProfileClick = { id -> onProfileClick(id) },
                            onPlaySong = onPlaySong,
                            onVotePoll = { pid, oid -> onVotePoll(pid, oid) },
                            onMediaClick = onMediaClick,
                            onShareClick = onShareClick,
                            onFollowClick = onFollowUser
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)

                        // REQUIREMENT: Inject 1 native ad unit every 4 content posts
                        if ((index + 1) % 4 == 0) {
                            val adIndex = (index + 1) / 4 - 1
                            if (adIndex < preloadedAds.size) {
                                com.example.firstapp.common.ui.components.StaticAdCard(
                                    nativeAd = preloadedAds[adIndex],
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                DynamicAdCard(
                                    adManager = adManager,
                                    adUnitId = "ca-app-pub-3940256099942544/2247696110",
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    onAdRevenuePaid = { rev ->
                                        onAdRevenueLogged(post.id, rev, "main_feed")
                                    }
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                        }
                }
            }
            
            // Block fetching more until first 30 posts and ads are loaded
            if (posts.itemCount >= 30 && !adsLoaded) {
                 item {
                     Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator(color = AppPrimary)
                     }
                 }
            }
        }
    }
}

@Composable
fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().height(64.dp), color = MaterialTheme.colorScheme.background, tonalElevation = 4.dp) {
        TextField(
            value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxSize(),
            placeholder = { Text("Search profiles or posts...", color = Color.Gray) },
            leadingIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            trailingIcon = { if (query.isNotEmpty()) { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null) } } },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, 
                unfocusedContainerColor = Color.Transparent, 
                cursorColor = MaterialTheme.colorScheme.primary, 
                focusedTextColor = MaterialTheme.colorScheme.onBackground, 
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

fun Post.authorToUser() = User(id = authorId, username = authorHandle, displayName = authorName, avatarUrl = authorAvatarUrl, isVerified = isVerifiedAuthor)

fun copyUriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val mimeType = context.contentResolver.getType(uri)
        val extension = if (mimeType != null) {
            android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "tmp"
        } else {
            "tmp"
        }
        
        val tempFile = File.createTempFile("upload_", ".$extension", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }
        tempFile
    } catch (e: Exception) { null }
}
