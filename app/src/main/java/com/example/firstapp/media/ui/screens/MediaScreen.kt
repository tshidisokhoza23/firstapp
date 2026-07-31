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
    onCreatePost: (content: String, mediaUrls: List<String>, tags: List<String>, postType: String?, pollOptions: List<String>?, location: String?) -> Unit,
    onLikePost: (postId: String, likes: Long, isLiked: Boolean) -> Unit,
    onRepostPost: (postId: String) -> Unit,
    onReplySubmit: (postId: String, parentReplyId: String?, content: String) -> Unit,
    onReplyLike: (replyId: String) -> Unit,
    onReplyRepost: (replyId: String) -> Unit,
    onReplyDelete: (replyId: String) -> Unit = {},
    onBookmarkPost: (postId: String) -> Unit,
    onBookmarkReply: (replyId: String) -> Unit,
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
    onMediaClick: (String, String) -> Unit = { _, _ -> },
    onShareClick: (Post) -> Unit = {},
    onProfileClick: (String) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
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
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                topBar = {
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
                                Text(
                                    "Media", 
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { isSearchActive = true }
                                ) 
                            },
                            navigationIcon = {
                                Box(
                                    modifier = Modifier
                                        .padding(start = 12.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(ownProfileAvatarColor)
                                        .clickable { 
                                            onProfileClick(ownProfileId)
                                        }
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
                            actions = {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                                }
                                IconButton(onClick = { showCreatePost = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Create Post")
                                }
                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                                    }
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        containerColor = AppSecondary
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Direct Messages", color = Color.White) },
                                            leadingIcon = { Icon(Icons.Default.Mail, null, tint = Color.White) },
                                            onClick = { 
                                                showMenu = false
                                                onDirectMessagesClick()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Settings", color = Color.White) },
                                            leadingIcon = { Icon(Icons.Default.Settings, null, tint = Color.White) },
                                            onClick = { showMenu = false }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = AppBackground,
                                titleContentColor = Color.White,
                                navigationIconContentColor = Color.White,
                                actionIconContentColor = Color.White
                            ),
                            scrollBehavior = scrollBehavior,
                            windowInsets = WindowInsets(0, 0, 0, 0) // Takes icons higher by removing default status bar insets
                        )
                    }
                },
                containerColor = AppBackground,
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

        if (showRepostOptions != null) {
            val targetPost = showRepostOptions!!
            ModalBottomSheet(
                onDismissRequest = { showRepostOptions = null },
                containerColor = AppSecondary
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    ListItem(
                        headlineContent = { Text(if (targetPost.isReposted) "Undo Repost" else "Repost", color = Color.White) },
                        leadingContent = { Icon(Icons.Default.Repeat, null, tint = if (targetPost.isReposted) Color(0xFF00BA7C) else Color.White) },
                        modifier = Modifier.clickable {
                            onRepostPost(targetPost.id)
                            showRepostOptions = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Quote Post", color = Color.White) },
                        leadingContent = { Icon(Icons.Default.Edit, null, tint = Color.White) },
                        modifier = Modifier.clickable {
                            // TODO: Open composer with quote
                            // mediaViewModel.setQuotedPost(targetPost)
                            // showCreatePost = true
                            showRepostOptions = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        if (showCreatePost) {
            CreatePostDialog(
                avatarUrl = ownProfileAvatarUrl,
                avatarColor = ownProfileAvatarColor,
                onDismiss = { showCreatePost = false },
                onSubmit = { content, media, tags, type, poll, loc ->
                    onCreatePost(content, media, tags, type, poll, loc)
                    showCreatePost = false
                }
            )
        }

        if (replyingToPost != null) {
            ReplyDialog(
                post = replyingToPost!!,
                reply = replyingToReply,
                onDismiss = { 
                    replyingToPost = null
                    replyingToReply = null
                },
                onSubmit = { content ->
                    onReplySubmit(replyingToPost!!.id, replyingToReply?.id, content)
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
    onMediaClick: (String, String) -> Unit = { _, _ -> },
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
                Text(text = "Could not load media feed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error.localizedMessage ?: "Connection error", color = SecondaryText, fontSize = 14.sp)
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
                Text(text = "No posts yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            return
        }
    }

    PullToRefreshBox(
        isRefreshing = posts.loadState.refresh is LoadState.Loading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
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
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

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
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
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
    Surface(modifier = Modifier.fillMaxWidth().height(64.dp), color = AppBackground, tonalElevation = 4.dp) {
        TextField(
            value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxSize(),
            placeholder = { Text("Search profiles or posts...", color = Color.Gray) },
            leadingIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } },
            trailingIcon = { if (query.isNotEmpty()) { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null, tint = Color.White) } } },
            singleLine = true,
            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = AppAccent, focusedTextColor = Color.White, unfocusedTextColor = Color.White)
        )
    }
}

@Composable
fun ReplyDialog(post: Post, reply: Post?, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reply to @${reply?.authorHandle ?: post.authorHandle}") },
        text = { TextField(value = content, onValueChange = { content = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Post your reply") }) },
        confirmButton = { Button(onClick = { onSubmit(content) }) { Text("Reply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = SurfaceColor, titleContentColor = Color.White, textContentColor = Color.White
    )
}

@Composable
fun CreatePostDialog(
    avatarUrl: String? = null,
    avatarColor: Color = Color.Gray,
    onDismiss: () -> Unit,
    onSubmit: (String, List<String>, List<String>, String?, List<String>?, String?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var selectedAudioUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var showPoll by remember { mutableStateOf(false) }
    var pollOptions by remember { mutableStateOf<List<String>>(listOf("", "")) }
    val context = LocalContext.current
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            val current = selectedMediaUris.toMutableList()
            for (uri in uris) { if (!current.contains(uri)) current.add(uri) }
            selectedMediaUris = current
        }
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            val current = selectedAudioUris.toMutableList()
            for (uri in uris) { if (!current.contains(uri)) current.add(uri) }
            selectedAudioUris = current
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.7f).padding(bottom = 80.dp), shape = RoundedCornerShape(24.dp), color = AppBackground, border = BorderStroke(1.dp, DividerColor.copy(alpha = 0.2f))) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
                        Button(onClick = {
                            val mediaList = ArrayList<String>()
                            for (u in selectedMediaUris) mediaList.add(u.toString())
                            for (u in selectedAudioUris) mediaList.add(u.toString())
                            
                            val isSingleVideo = selectedMediaUris.size == 1 && context.contentResolver.getType(selectedMediaUris[0])?.startsWith("video") == true
                            val postType = when { 
                                showPoll -> "poll"
                                isSingleVideo -> "short"
                                selectedMediaUris.isNotEmpty() -> "standard"
                                selectedAudioUris.isNotEmpty() -> "standard"
                                else -> "standard"
                            }

                            val finalPoll = if (showPoll) pollOptions.filter { it.isNotBlank() } else null
                            onSubmit(content, mediaList, emptyList<String>(), postType, finalPoll, selectedLocation)
                        }, enabled = content.isNotEmpty() || selectedMediaUris.isNotEmpty() || selectedAudioUris.isNotEmpty() || showPoll, colors = ButtonDefaults.buttonColors(containerColor = AppPrimary, contentColor = Color.White), shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(end = 8.dp).height(36.dp)) { Text("Post", fontWeight = FontWeight.Bold) }
                    }
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(avatarColor)) {
                                if (avatarUrl != null) { AsyncImage(model = avatarUrl, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                else { Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.align(Alignment.Center)) }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(value = content, onValueChange = { content = it }, placeholder = { Text("What's happening?", color = SecondaryText, fontSize = 18.sp) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, cursorColor = AppPrimary, focusedTextColor = Color.White, unfocusedTextColor = Color.White), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp))
                        }
                        if (selectedMediaUris.isNotEmpty() || selectedAudioUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(selectedMediaUris) { uri ->
                                    val mimeType = context.contentResolver.getType(uri)
                                    val isVideo = mimeType?.startsWith("video") == true
                                    Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(Color.DarkGray)) {
                                        AsyncImage(model = uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        if (isVideo) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)) }
                                        IconButton(onClick = { selectedMediaUris = selectedMediaUris.filter { it != uri } }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                    }
                                }
                            }
                        }
                        if (selectedLocation != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.background(AppPrimary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = AppPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(selectedLocation!!, color = AppPrimary, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Close, null, tint = AppPrimary, modifier = Modifier.size(14.dp).clickable { selectedLocation = null })
                            }
                        }
                        if (showPoll) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceColor), border = BorderStroke(1.dp, DividerColor), shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    pollOptions.forEachIndexed { index, option ->
                                        OutlinedTextField(value = option, onValueChange = { newValue -> val newList = pollOptions.toMutableList(); newList[index] = newValue; pollOptions = newList }, placeholder = { Text("Choice ${index + 1}") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White), shape = RoundedCornerShape(8.dp))
                                    }
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.fillMaxWidth().imePadding()) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clickable { }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, null, tint = AppPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Everyone can reply", color = AppPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { Icon(Icons.Default.Image, null, tint = AppPrimary, modifier = Modifier.size(20.dp)) }
                            IconButton(onClick = { if (!showPoll) { showPoll = true; selectedMediaUris = emptyList(); selectedAudioUris = emptyList() } }) { Icon(Icons.Default.Poll, null, tint = AppPrimary, modifier = Modifier.size(20.dp)) }
                            IconButton(onClick = { selectedLocation = "Cape Town, SA" }) { Icon(Icons.Default.LocationOn, null, tint = AppPrimary, modifier = Modifier.size(20.dp)) }
                        }
                    }
                }
            }
        }
    }
}

fun Post.authorToUser() = User(id = authorId, username = authorHandle, displayName = authorName, avatarUrl = authorAvatarUrl, isVerified = isVerifiedAuthor)

fun copyUriToTempFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload_", ".tmp", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }
        tempFile
    } catch (e: Exception) { null }
}
