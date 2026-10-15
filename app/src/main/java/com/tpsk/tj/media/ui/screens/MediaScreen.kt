@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tpsk.tj.media.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.tpsk.tj.common.model.User
import com.tpsk.tj.home.ui.components.AppLogo
import java.io.File

import java.io.FileOutputStream
import kotlin.text.*
import com.tpsk.tj.media.model.*
import com.tpsk.tj.music.model.Song
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.common.util.formatTwitterTime
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.ui.components.*
import com.tpsk.tj.chat.data.CallType
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
import kotlinx.coroutines.flow.distinctUntilChanged
import com.tpsk.tj.common.data.ads.AdManager
import com.tpsk.tj.common.data.ads.AdUnitConfig
import com.tpsk.tj.common.ui.components.DynamicAdCard
import com.tpsk.tj.common.data.ads.AdMobManagerImpl // Temporary for non-injected usage if needed
import com.tpsk.tj.ui.navigation.Screen
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.*
import kotlin.collections.*
import com.tpsk.tj.profile.ui.screens.ProfileView
import com.tpsk.tj.common.ui.components.PostItemShimmer
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
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
    onCreatePost: (content: String, mediaUrls: List<String>, tags: List<String>, postType: String?, pollOptions: List<String>?, location: String?, parentPostId: String?, rootPostId: String?, repostOfId: String?) -> Unit,
    onLikePost: (postId: String, likes: Long, isLiked: Boolean) -> Unit,
    onRepostPost: (Post) -> Unit,
    onBookmarkPost: (postId: String) -> Unit,
    onPostViewed: (postId: String) -> Unit = {},
    onProfileViewChange: (Boolean) -> Unit = {},
    onGetUserPosts: (String, String) -> Flow<PagingData<Post>> = { _, _ -> kotlinx.coroutines.flow.flowOf() },
    userViewModel: UserViewModel,
    mediaViewModel: MediaViewModel,
    onPlaySong: (Song, List<Song>) -> Unit = { _, _ -> },
    onVotePoll: (postId: String, optionId: String) -> Unit = { _, _ -> },
    onFollowUser: (userId: String, isFollowed: Boolean) -> Unit = { _, _ -> },
    onCallClick: (userId: String, CallType) -> Unit = { _, _ -> },
    onDeletePost: (postId: String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onCreatePostClick: Int = 0,
    onRefresh: () -> Unit = {},
    refreshTrigger: Int = 0,
    onDirectMessagesClick: () -> Unit = {},
    onSavedPostsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAdRevenueLogged: (String, Double, String) -> Unit = { _, _, _ -> },
    onShortClick: (Post) -> Unit = {},
    onMediaClick: (String, String, Long) -> Unit = { _, _, _ -> },
    onShareClick: (Post) -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    onNavigateToCircles: () -> Unit = {},
    onNavigateToCreatorDashboard: () -> Unit = {},
    autoplayEnabled: Boolean = true,
    autoplayGifsEnabled: Boolean = true,
    onRequestCreatePost: (replyId: String?, quoteId: String?) -> Unit = { _, _ -> },
    categories: List<Category> = emptyList(),
    selectedCategoryId: String? = null,
    onCategorySelected: (String?) -> Unit = {},
    selectedQualityMode: String = "balanced",
    onQualityModeSelected: (String) -> Unit = {},
    onContestsClick: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    var lastRefreshTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(refreshTrigger) {
        val now = System.currentTimeMillis()
        if (refreshTrigger > 0 && now - lastRefreshTime > 1000) {
            lastRefreshTime = now
            posts.refresh()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchGlobal by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, searchGlobal) {
        if (isSearchActive) {
            mediaViewModel.search(searchQuery, searchGlobal)
        }
    }

    var showRepostOptions by remember { mutableStateOf<Post?>(null) }
    var showTopMoreSheet by remember { mutableStateOf(false) }

    LaunchedEffect(openOwnProfileRequest) {
        if (openOwnProfileRequest > 0) {
            onProfileClick(ownProfileId)
        }
    }

    var reportingPost by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(onCreatePostClick) {
        if (onCreatePostClick > 0) {
            onRequestCreatePost(null, null)
        }
    }

    LaunchedEffect(closeOwnProfileRequest) {
        if (closeOwnProfileRequest > 0) {
            onProfileViewChange(false)
        }
    }

    val preloadedAds by mediaViewModel.preloadedAds.collectAsState()
    val adsLoaded by mediaViewModel.adsLoaded.collectAsState()

    var showPostMoreOptions by remember { mutableStateOf<Post?>(null) }
    var postContextText by remember { mutableStateOf<String?>(null) }
    var postContextId by remember { mutableStateOf<String?>(null) }

    val backgroundBrush = if (LocalIsDarkTheme.current) {
        Brush.verticalGradient(
            colors = listOf(AppBackgroundDarkTop, AppBackgroundDarkBottom)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color.White, AppBackgroundLight)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        androidx.activity.compose.BackHandler(
            enabled = isSearchActive || showRepostOptions != null || showPostMoreOptions != null || reportingPost != null
        ) {
            when {
                isSearchActive -> {
                    isSearchActive = false
                    searchQuery = ""
                }
                showRepostOptions != null -> showRepostOptions = null
                showPostMoreOptions != null -> showPostMoreOptions = null
                reportingPost != null -> reportingPost = null
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier,
                topBar = {
                    Box(modifier = Modifier.background(Color.Transparent).statusBarsPadding()) {
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
                            CenterAlignedTopAppBar(
                                title = {
                                    AppLogo(size = 36.dp)
                                },
                                navigationIcon = {
                                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                                    DisposableEffect(lifecycleOwner) {
                                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                                mediaViewModel.fetchNotificationsCount()
                                            }
                                        }
                                        lifecycleOwner.lifecycle.addObserver(observer)
                                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                                    }
                                    val unreadNotifsCount by mediaViewModel.unreadNotificationCount.collectAsState()

                                    BadgedBox(
                                        badge = {
                                            if (unreadNotifsCount > 0) {
                                                Badge(
                                                    containerColor = Color(0xFFD32F2F),
                                                    contentColor = Color.White
                                                ) {
                                                    Text(
                                                        text = if (unreadNotifsCount > 99) "99+" else "$unreadNotifsCount",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        IconButton(onClick = { 
                                            mediaViewModel.fetchNotificationsCount()
                                            showTopMoreSheet = true 
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Menu",
                                                tint = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Search")
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color.Transparent,
                                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                    }
                },
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    if (isSearchActive && searchQuery.isNotEmpty()) {
                        val searchResults by mediaViewModel.searchResults.collectAsState()
                        SearchResultsList(
                            results = searchResults,
                            autoplayEnabled = autoplayEnabled,
                            autoplayGifsEnabled = autoplayGifsEnabled,
                            onPostClick = onPostSelect,
                            onProfileClick = onProfileClick
                        )
                    } else {
                        MainFeedSection(
                            bottomPadding = bottomPadding,
                            adManager = adManager,
                            posts = posts,
                            preloadedAds = preloadedAds,
                            adsLoaded = adsLoaded,
                            ownProfileId = ownProfileId,
                            ownProfileHandle = ownProfileHandle,
                            autoplayEnabled = autoplayEnabled,
                            autoplayGifsEnabled = autoplayGifsEnabled,
                            onPostClick = onPostSelect,
                            onLike = onLikePost,
                            onRepostClick = { showRepostOptions = it },
                            onBookmark = onBookmarkPost,
                            onDeletePost = onDeletePost,
                            onMoreClick = { showPostMoreOptions = it },
                            onReplyClick = { p, r -> 
                                onRequestCreatePost(r?.id ?: p.id, null)
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
                            onFollowUser = onFollowUser,
                            onPostViewed = onPostViewed,
                            categories = categories,
                            selectedCategoryId = selectedCategoryId,
                            onCategorySelected = onCategorySelected,
                            selectedQualityMode = selectedQualityMode,
                            onQualityModeSelected = onQualityModeSelected,
                            onContestsClick = onContestsClick
                        )
                    }
                }
            }
        }


        if (showTopMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTopMoreSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("View Profile", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface) },
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
                            onRequestCreatePost(null, null)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Notifications", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showTopMoreSheet = false
                            onNotificationsClick()
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
                    HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 4.dp))
                    ListItem(
                        headlineContent = { Text("Circles", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.Group, null, tint = Color(0xFF00E5FF)) },
                        modifier = Modifier.clickable {
                            showTopMoreSheet = false
                            onNavigateToCircles()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Creator Dashboard", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold) },
                        leadingContent = { Icon(Icons.Default.Dashboard, null, tint = Color(0xFF00E5FF)) },
                        modifier = Modifier.clickable {
                            showTopMoreSheet = false
                            onNavigateToCreatorDashboard()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 4.dp))
                    ListItem(
                        headlineContent = { Text("Saved Posts", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showTopMoreSheet = false
                            onSavedPostsClick()
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(if (targetPost.isReposted) "Undo Repost" else "Repost", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Repeat, null, tint = if (targetPost.isReposted) Color(0xFF00BA7C) else MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.clickable {
                            onRepostPost(targetPost)
                            showRepostOptions = null
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Quote Post", color = MaterialTheme.colorScheme.onSurface) },
                        leadingContent = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.clickable {
                            showRepostOptions = null
                            onRequestCreatePost(null, targetPost.id)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        if (showPostMoreOptions != null) {
            val targetPost = showPostMoreOptions!!
            val context = LocalContext.current
            val isOwnPost = targetPost.isOwnedBy(ownProfileId, ownProfileHandle)

            PostOptionsMenu(
                post = targetPost,
                isOwner = isOwnPost,
                onReport = {
                    reportingPost = targetPost
                    showPostMoreOptions = null
                },
                onBlockUser = {
                    userViewModel.blockUser(targetPost.authorId)
                    mediaViewModel.hidePost(targetPost.id)
                    android.widget.Toast.makeText(context, "Blocked @${targetPost.authorHandle}", android.widget.Toast.LENGTH_SHORT).show()
                    showPostMoreOptions = null
                },
                onNotInterested = {
                    mediaViewModel.hidePost(targetPost.id)
                    android.widget.Toast.makeText(context, "We'll show fewer posts like this", android.widget.Toast.LENGTH_SHORT).show()
                    showPostMoreOptions = null
                },
                onDelete = {
                    mediaViewModel.deletePost(targetPost.id, ownProfileId, ownProfileHandle)
                    showPostMoreOptions = null
                },
                onTurnOffComments = {
                    mediaViewModel.toggleCommentsLock(targetPost.id, targetPost.repliesLocked)
                    showPostMoreOptions = null
                },
                onShare = {
                    onShareClick(targetPost)
                    showPostMoreOptions = null
                },
                onCopyLink = {
                    com.tpsk.tj.common.util.ShareUtils.copyPostLink(context, targetPost)
                    showPostMoreOptions = null
                },
                onBookmark = {
                    onBookmarkPost(targetPost.id)
                    showPostMoreOptions = null
                },
                onContext = {
                    val id = targetPost.id
                    val authorId = targetPost.authorId
                    showPostMoreOptions = null
                    postContextId = id
                    mediaViewModel.loadPostContext(id, authorId) { text -> postContextText = text }
                },
                onRequestCommission = {
                    val artistId = targetPost.authorId
                    showPostMoreOptions = null
                    mediaViewModel.requestCommission(artistId, "Commission request from your post") { message ->
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onDismiss = { showPostMoreOptions = null }
            )
        }

        if (postContextText != null) {
            AlertDialog(
                onDismissRequest = { postContextText = null },
                title = { Text("Post context") },
                text = { Text(postContextText.orEmpty()) },
                confirmButton = { TextButton(onClick = { postContextText = null }) { Text("Close") } },
                dismissButton = {
                    val noteId = Regex("[0-9a-fA-F-]{36}").find(postContextText.orEmpty())?.value
                    val postId = postContextId
                    if (noteId != null && postId != null) {
                        TextButton(onClick = {
                            mediaViewModel.voteNote(postId, noteId)
                            postContextText = null
                        }) { Text("Helpful") }
                    }
                }
            )
        }

        if (reportingPost != null) {
            val context = LocalContext.current
            val targetPost = reportingPost!!
            ReportContentDialog(
                post = targetPost,
                onDismiss = { reportingPost = null },
                onSubmitReport = { category, details ->
                    mediaViewModel.reportPost(targetPost.id, "$category: $details")
                    android.widget.Toast.makeText(context, "Report submitted. Thank you for keeping our community safe.", android.widget.Toast.LENGTH_LONG).show()
                    reportingPost = null
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
    onMoreClick: (Post) -> Unit,
    onReplyClick: (Post, Post?) -> Unit,
    onProfileClick: (String) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onVotePoll: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onAdRevenueLogged: (String, Double, String) -> Unit = { _, _, _ -> },
    onShortClick: (Post) -> Unit = {},
    onMediaClick: (String, String, Long) -> Unit = { _, _, _ -> },
    onShareClick: (Post) -> Unit = {},
    onFollowUser: (String, Boolean) -> Unit = { _, _ -> },
    onPostViewed: (String) -> Unit = {},
    autoplayEnabled: Boolean = true,
    autoplayGifsEnabled: Boolean = true,
    categories: List<Category> = emptyList(),
    selectedCategoryId: String? = null,
    onCategorySelected: (String?) -> Unit = {},
    selectedQualityMode: String = "balanced",
    onQualityModeSelected: (String) -> Unit = {},
    onContestsClick: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Category Chips Bar is ALWAYS rendered at the top of the feed
        CategoryChipBar(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = onCategorySelected,
            selectedQualityMode = selectedQualityMode,
            onQualityModeSelected = onQualityModeSelected,
            onContestsClick = onContestsClick
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                posts.loadState.refresh is LoadState.Loading && posts.itemCount == 0 -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(5) { PostItemShimmer() }
                    }
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
                        Button(
                            onClick = { posts.retry(); onRefresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6), contentColor = Color.White)
                        ) {
                            Text("Retry", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                posts.itemCount == 0 && posts.loadState.refresh is LoadState.NotLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "No posts yet", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(text = "Be the first to post!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = posts.loadState.refresh is LoadState.Loading,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val listState = rememberLazyListState()
                        var activePostId by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(listState) {
                            snapshotFlow { listState.isScrollInProgress }
                                .distinctUntilChanged()
                                .collect { isScrolling ->
                                    if (!isScrolling) {
                                        kotlinx.coroutines.delay(200)
                                        if (!listState.isScrollInProgress) {
                                            val layoutInfo = listState.layoutInfo
                                            val visibleItems = layoutInfo.visibleItemsInfo
                                            if (visibleItems.isNotEmpty()) {
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
                                                    activePostId = posts.peek(closestIndex)?.id
                                                }
                                            }
                                        }
                                    }
                                }
                        }

                        LaunchedEffect(activePostId) {
                            activePostId?.let { onPostViewed(it) }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp + bottomPadding)
                        ) {

            val organicCount = posts.itemCount
            val postsPerAd = 5
            val adSlots = organicCount / postsPerAd
            val rowCount = organicCount + adSlots
            items(
                count = rowCount,
                key = { row ->
                    val adsBefore = row / (postsPerAd + 1)
                    val isAd = adSlots > 0 && row % (postsPerAd + 1) == postsPerAd && adsBefore < adSlots
                    if (isAd) "feed-ad-$adsBefore" else {
                        val postIndex = row - adsBefore
                        posts.peek(postIndex)?.id?.let { "post-$it" } ?: "loading-post-$postIndex"
                    }
                },
                contentType = { row ->
                    val adsBefore = row / (postsPerAd + 1)
                    if (adSlots > 0 && row % (postsPerAd + 1) == postsPerAd && adsBefore < adSlots) "ad" else "post"
                }
            ) { row ->
                val adsBefore = row / (postsPerAd + 1)
                val isAd = adSlots > 0 && row % (postsPerAd + 1) == postsPerAd && adsBefore < adSlots
                if (isAd) {
                    com.tpsk.tj.common.ui.components.DynamicAdCard(
                        adManager = adManager,
                        adUnitId = AdUnitConfig.NATIVE_POSTS_ID,
                        slotKey = "feed-ad-$adsBefore",
                        reserveSlot = true,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                } else {
                    val postIndex = row - adsBefore
                    val post = if (postIndex < posts.itemCount) posts[postIndex] else null
                    if (post == null) {
                        PostItemShimmer()
                    } else {
                        val isPlaying = activePostId != null && activePostId == post.id
                        val isOwn = remember(post.authorId, post.authorHandle, ownProfileId, ownProfileHandle) {
                            post.authorId == ownProfileId || (ownProfileHandle.isNotEmpty() && post.authorHandle == ownProfileHandle)
                        }
                        PostItem(
                            adManager = adManager,
                            post = post,
                            isOwnPost = isOwn,
                            showThreadLine = post.hasThread,
                            isLastInThread = true,
                            activePostId = activePostId,
                            isVideoPlaying = isPlaying,
                            autoplayEnabled = autoplayEnabled,
                            autoplayGifsEnabled = autoplayGifsEnabled,
                            onClick = { onPostClick(post) },
                            onQuotedPostClick = { quotedPost -> onPostClick(quotedPost) },
                            onLike = { id, likes, liked -> onLike(id, likes, liked) },
                            onRepostClick = { p -> onRepostClick(p) },
                            onBookmark = { id -> onBookmark(id) },
                            onDelete = { id -> onDeletePost(id) },
                            onMoreClick = onMoreClick,
                            onReplyClick = { p, r -> onReplyClick(p, r) },
                            onProfileClick = { id -> onProfileClick(id) },
                            onPlaySong = onPlaySong,
                            onVotePoll = { pid, oid -> onVotePoll(pid, oid) },
                            onMediaClick = onMediaClick,
                            onShareClick = onShareClick,
                            onFollowClick = onFollowUser
                        )
                    }
                }
            }

            if (posts.loadState.append is LoadState.Loading) {
                item(key = "feed-loading-more", contentType = "loading-more") {
                    PostItemShimmer()
                }
            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTopBar(
    query: String, 
    searchGlobal: Boolean = false,
    onQueryChange: (String) -> Unit, 
    onToggleGlobal: () -> Unit = {},
    onClose: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth().height(64.dp), color = MaterialTheme.colorScheme.background, tonalElevation = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query, onValueChange = onQueryChange, modifier = Modifier.weight(1f),
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
            
            TextButton(
                onClick = onToggleGlobal,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = if (searchGlobal) "Global" else "Contacts",
                    color = if (searchGlobal) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun SearchResultsList(
    results: Resource<List<Post>>,
    autoplayEnabled: Boolean = true,
    autoplayGifsEnabled: Boolean = true,
    onPostClick: (Post) -> Unit,
    onProfileClick: (String) -> Unit
) {
    when (results) {
        is Resource.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppPrimary)
            }
        }
        is Resource.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(text = results.message ?: "Search failed", color = MaterialTheme.colorScheme.error)
            }
        }
        is Resource.Success -> {
            val postsList = results.data ?: emptyList()
            if (postsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(postsList) { post ->
                        VerticalFeedPostCard(
                            post = post,
                            autoplayEnabled = autoplayEnabled,
                            autoplayGifsEnabled = autoplayGifsEnabled,
                            onLikeClick = {},
                            onCommentClick = {},
                            onRepostClick = {},
                            onShareClick = {},
                            onBookmarkClick = {},
                            onFollowClick = {},
                            onMoreClick = {},
                            onProfileClick = onProfileClick,
                            onMediaClick = { _, _, _ -> },
                            modifier = Modifier.clickable { onPostClick(post) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

fun Post.authorToUser() = User(id = authorId, username = authorHandle, displayName = authorName, avatarUrl = authorAvatarUrl, isVerified = isVerifiedAuthor)
