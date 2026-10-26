@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.tpsk.tj.profile.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.tpsk.tj.common.model.*
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import com.tpsk.tj.media.ui.viewmodel.SharedMediaViewModel
import com.tpsk.tj.common.util.MediaUtils
import com.tpsk.tj.media.ui.components.PostItem
import com.tpsk.tj.media.ui.components.PostOptionsMenu
import com.tpsk.tj.media.ui.components.ReportContentDialog
import com.tpsk.tj.media.model.isOwnedBy
import android.widget.Toast
import com.tpsk.tj.common.util.Resource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.paging.LoadState
import com.tpsk.tj.chat.model.Contact
import java.util.ArrayList
import kotlin.collections.*
import kotlin.ranges.*
import kotlin.text.*

@Composable
fun ProfileView(
    userId: String,
    isGroup: Boolean = false,
    onBack: () -> Unit,
    posts: LazyPagingItems<Post>,
    shorts: LazyPagingItems<Post>,
    mediaItems: LazyPagingItems<Post>,
    replies: LazyPagingItems<Post>,
    userViewModel: UserViewModel,
    mediaViewModel: MediaViewModel,
    adManager: com.tpsk.tj.common.data.ads.AdManager,
    sharedMediaViewModel: SharedMediaViewModel = hiltViewModel(),
    onNavigateToSharedMedia: (String) -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMore: () -> Unit = {},
    onShortClick: (Post) -> Unit = {},
    onNavigateToMediaViewer: (String, String, Long) -> Unit = { _, _, _ -> },
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToFollowList: (String, String) -> Unit = { _, _ -> },
    initialTab: String? = "Posts"
) {
    val context = LocalContext.current
    val currentUser = com.tpsk.tj.LocalCurrentUser.current
    
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        uri?.let { userViewModel.onAvatarSelected(context, it) }
    }

    var remoteUser by remember(userId) { mutableStateOf<User?>(null) }

    // Single Source of Truth for ID comparison
    val isMyProfile = remember(userId, currentUser.id, currentUser.username, remoteUser) { 
        val u = userId.toString().trim()
        u.isEmpty() || u == "null" || u == currentUser.id || u.equals(currentUser.username, ignoreCase = true) || (remoteUser != null && (remoteUser?.id == currentUser.id || remoteUser?.username.equals(currentUser.username, ignoreCase = true)))
    }
    var profileRefreshNonce by remember(userId) { mutableIntStateOf(0) }
    var isFollowUpdating by remember(userId) { mutableStateOf(false) }
    val dismissedSuggestedUserIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(isMyProfile) {
        if (isMyProfile) {
            userViewModel.fetchCurrentUser()
        }
    }

    LaunchedEffect(userId, profileRefreshNonce) {
        val u = userId.toString()
        if (u.length > 0 && u != "null") {
            userViewModel.getUserById(u).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    remoteUser = resource.data
                }
            }
        }
    }

    // "Self-Healing" logic: If this is my profile, use the session user immediately.
    // This prevents gray loading boxes when navigating to your own profile.
    val user = remember(isMyProfile, currentUser, remoteUser) {
        if (isMyProfile) {
            remoteUser ?: currentUser
        } else {
            remoteUser
        }
    }



    var selectedPostForMore by remember { mutableStateOf<Post?>(null) }
    var reportingPost by remember { mutableStateOf<Post?>(null) }

    var selectedTabIndex by remember(userId, initialTab) { 
        mutableIntStateOf(
            when (initialTab?.lowercase()) {
                "shorts" -> 3
                "media" -> 2
                "replies" -> 1
                else -> 0
            }
        )
    }
    val tabs = remember(remoteUser, currentUser) {
        val l = ArrayList<String>()
        l.add("Posts")
        l.add("Replies")
        l.add("Media")
        l.add("Portfolio")
        l.add("Shorts")
        l
    }
    
    val safeSelectedTabIndex = if (selectedTabIndex < tabs.size) selectedTabIndex else 0
    val selectedTab = if (safeSelectedTabIndex < tabs.size) tabs.get(safeSelectedTabIndex) else if (tabs.size > 0) tabs.get(tabs.size - 1) else ""
    val profileOwnerId = if (userId.length > 0 && userId != "null") userId else currentUser.id
    var reputation by remember(profileOwnerId) { mutableIntStateOf(50) }
    LaunchedEffect(profileOwnerId) {
        mediaViewModel.loadReputation(profileOwnerId) { reputation = it }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val selectedTabItems = remember(selectedTab, posts, shorts, mediaItems, replies) {
        when (selectedTab) {
            "Shorts" -> shorts
            "Media" -> mediaItems
            "Replies" -> replies
            else -> posts
        }
    }

    val isRefreshing = selectedTabItems.loadState.refresh is LoadState.Loading

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
                CenterAlignedTopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onNavigateToNotifications) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications")
                        }
                        IconButton(onClick = onNavigateToMore) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { 
                profileRefreshNonce += 1
                selectedTabItems.refresh() 
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            val listState = rememberLazyListState()
            val activePostId by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    if (visibleItems.size == 0) return@derivedStateOf null
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
                    val itemOffset = 2
                    if (closestIndex >= itemOffset && (closestIndex - itemOffset) < selectedTabItems.itemCount) {
                        selectedTabItems.peek(closestIndex - itemOffset)?.id
                    } else null
                }
            }

            LaunchedEffect(activePostId) {
                activePostId?.let { mediaViewModel.viewPost(it) }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (user != null) {
                                    val nameToShow = if (user.displayName.isNotEmpty()) user.displayName else "No Name"
                                    Text(
                                        text = nameToShow,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        text = "@${user.username}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {
                                    Box(modifier = Modifier.width(150.dp).height(24.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                                    Box(modifier = Modifier.padding(top = 4.dp).width(100.dp).height(16.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(4.dp)))
                                }
                            }

                        val gradientBrush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF8A2BE2), Color(0xFF00E5FF))
                        )
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .border(2.dp, gradientBrush, CircleShape)
                                    .padding(3.dp)
                            ) {
                                if (user != null && !user.avatarUrl.isNullOrBlank()) {
                                     val url = user.avatarUrl!!
                                     AsyncImage(
                                        model = MediaUtils.formatUrl(url),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(AppSecondary)
                                            .clickable { 
                                                if (isMyProfile) {
                                                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                } else {
                                                    onNavigateToMediaViewer(url, "image", 0L)
                                                }
                                            },
                                        contentScale = ContentScale.Crop,
                                        error = androidx.compose.ui.graphics.painter.ColorPainter(AppSecondary),
                                        fallback = androidx.compose.ui.graphics.painter.ColorPainter(AppSecondary)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(AppSecondary)
                                            .clickable {
                                                if (isMyProfile) {
                                                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, null, tint = Color.White)
                                    }
                                }
                            }

                            if (user?.isArtist == true || isMyProfile) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .background(gradientBrush, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Artist",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        }

                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onNavigateToFollowList(profileOwnerId, "followers") }
                            ) {
                                Text("${user?.followersCount ?: 0}", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Text("Followers", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                            Box(modifier = Modifier.size(2.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onNavigateToFollowList(profileOwnerId, "following") }
                            ) {
                                Text("${user?.followingCount ?: 0}", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Text("Following", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                            Box(modifier = Modifier.size(2.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Reputation $reputation", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (user?.isArtist == true || isMyProfile) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF00E676))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00E676)))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open for Commissions", color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                listOf("Digital Character Design", "Concept Art", "Illustration").forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF242424),
                                        border = BorderStroke(1.dp, Color(0xFF333333))
                                    ) {
                                        Text(tag, color = Color(0xFF00E5FF), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { onNavigateToMore() },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                border = BorderStroke(1.dp, Color(0xFF333333))
                            ) {
                                Text("Circles", fontSize = 12.sp, color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { onNavigateToMore() },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                border = BorderStroke(1.dp, Color(0xFF333333))
                            ) {
                                Text("Analytics", fontSize = 12.sp, color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { onNavigateToMore() },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f).height(36.dp),
                                border = BorderStroke(1.dp, Color(0xFF333333))
                            ) {
                                Text("Monetize", fontSize = 12.sp, color = Color.White)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isMyProfile) {
                                Button(
                                    onClick = onNavigateToEditProfile,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onBackground, 
                                        contentColor = MaterialTheme.colorScheme.background
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Edit profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            } else if (user != null) {
                                val isFollowing = user.isFollowing
                                Button(
                                    onClick = {
                                        if (isFollowUpdating) return@Button
                                        isFollowUpdating = true
                                        if (isFollowing) {
                                            userViewModel.unfollowUser(user.id) { success ->
                                                isFollowUpdating = false
                                                if (success) profileRefreshNonce += 1
                                            }
                                        } else {
                                            userViewModel.followUser(user.id) { success ->
                                                isFollowUpdating = false
                                                if (success) profileRefreshNonce += 1
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) Color.Transparent else MaterialTheme.colorScheme.onBackground,
                                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background
                                    ),
                                    border = if (isFollowing) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    if (isMyProfile) {
                                        val shareUrl = "https://firstapp.app/u/${user?.username ?: currentUser.username}"
                                        val shareText = "Check out my profile on FirstApp: $shareUrl"
                                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                        }
                                        val chooser = android.content.Intent.createChooser(intent, "Share profile")
                                        context.startActivity(chooser)
                                    } else if (user != null) {
                                        com.tpsk.tj.common.util.GlobalNavigation.navigateTo(
                                            com.tpsk.tj.ui.navigation.Screen.Chat.createRoute(user.id, type = "initiate")
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.onBackground
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isMyProfile) "Share profile" else "Message", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        if (isMyProfile) {
                            val uiState by mediaViewModel.uiState.collectAsState()
                            val suggestedUsers = uiState.suggestedUsers
                            
                            val visibleSuggestedUsers = suggestedUsers.filterNot { it.id in dismissedSuggestedUserIds }
                            if (visibleSuggestedUsers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Suggested for you",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(visibleSuggestedUsers) { contact ->
                                        SuggestedUserCard(
                                            contact = contact,
                                            onDismiss = {
                                                if (!dismissedSuggestedUserIds.contains(contact.id)) {
                                                    dismissedSuggestedUserIds.add(contact.id)
                                                }
                                            },
                                            onFollow = {
                                                if (!dismissedSuggestedUserIds.contains(contact.id)) {
                                                    dismissedSuggestedUserIds.add(contact.id)
                                                }
                                                mediaViewModel.followUser(contact.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                        TabRow(
                            selectedTabIndex = safeSelectedTabIndex,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) },
                            indicator = { tabPositions ->
                                if (safeSelectedTabIndex < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[safeSelectedTabIndex]),
                                        color = MaterialTheme.colorScheme.primary,
                                        height = 2.dp
                                    )
                                }
                            }
                        ) {
                            for (idx in 0..tabs.size - 1) {
                                val title = tabs.get(idx)
                                Tab(
                                    selected = safeSelectedTabIndex == idx,
                                    onClick = { selectedTabIndex = idx },
                                    text = { 
                                        Text(
                                            text = title, 
                                            fontSize = 14.sp, 
                                            fontWeight = if (safeSelectedTabIndex == idx) FontWeight.Bold else FontWeight.Medium,
                                            color = if (safeSelectedTabIndex == idx) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) 
                                    }
                                )
                            }
                        }
                }

                when (selectedTab) {
                    "Posts" -> {
                        if (posts.itemCount == 0 && posts.loadState.refresh is LoadState.NotLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Text("No posts yet", color = AppTextSecondary)
                                }
                            }
                        } else {
                            items(
                                count = posts.itemCount,
                                key = posts.itemKey { it?.id ?: 0 }
                            ) { index ->
                                val post = posts[index]
                                if (post != null) {
                                    PostItem(
                                        adManager = adManager,
                                        post = post,
                                        isOwnPost = post.authorId == currentUser.id || (currentUser.username.length > 0 && post.authorHandle == currentUser.username),
                                        activePostId = activePostId,
                                        onClick = { onNavigateToPostDetail(post.id) },
                                        onQuotedPostClick = { quotedPost -> onNavigateToPostDetail(quotedPost.id) },
                                        onLike = { id, likes, liked -> mediaViewModel.likePost(id, likes, liked) },
                                        onRepostClick = { p -> mediaViewModel.repostPost(p.id, p.repostCount, p.isReposted) },
                                        onBookmark = { mediaViewModel.bookmarkPost(it) },
                                        onDelete = { mediaViewModel.deletePost(post.id) },
                                        onMoreClick = { selectedPostForMore = it },
                                        onReplyClick = { p, r -> onNavigateToPostDetail(p.id) },
                                        onProfileClick = { targetId ->
                                            if (targetId.isNotBlank() && targetId != "null" && targetId != userId) {
                                                com.tpsk.tj.common.util.GlobalNavigation.navigateTo(
                                                    com.tpsk.tj.ui.navigation.Screen.UserProfile.createRoute(targetId)
                                                )
                                            }
                                        },
                                        onHashtagClick = { tag ->
                                            val clean = tag.removePrefix("#").trim()
                                            if (clean.isNotBlank()) {
                                                com.tpsk.tj.common.util.GlobalNavigation.navigateTo(
                                                    com.tpsk.tj.media.ui.components.hashtagRoute(clean)
                                                )
                                            }
                                        },
                                        onVotePoll = { pid, oid -> mediaViewModel.votePoll(pid, oid) },
                                        onPlaySong = { _, _ -> },
                                        onMediaClick = { url, type, pos -> onNavigateToMediaViewer(url, type, pos) },
                                        onShareClick = { p -> 
                                            mediaViewModel.sharePost(p.id, p.shares)
                                            com.tpsk.tj.common.util.ShareUtils.sharePost(context, p) 
                                        },
                                        onFollowClick = { id, followed ->
                                            if (followed) mediaViewModel.unfollowUser(id)
                                            else mediaViewModel.followUser(id)
                                        }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                    "Portfolio" -> {
                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    listOf("All", "Available", "Sold", "Process").forEachIndexed { idx, filter ->
                                        Surface(
                                            onClick = {},
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (idx == 0) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF242424),
                                            border = BorderStroke(1.dp, if (idx == 0) Color(0xFF00E5FF) else Color(0xFF333333))
                                        ) {
                                            Text(
                                                filter,
                                                color = if (idx == 0) Color(0xFF00E5FF) else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                val portfolioItems = listOf(
                                    Triple("Cyberpunk Neon Character", "1.2K Views • 45 Tips", "Series: Neo-Tokyo"),
                                    Triple("Ethereal Landscape", "850 Views • 22 Tips", "Series: Mythos"),
                                    Triple("Mecha Unit 04 Concept", "2.1K Views • 89 Tips", "Series: Mecha Warfare"),
                                    Triple("Portrait Study #12", "600 Views • 15 Tips", "Series: Daily Sketches")
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    portfolioItems.chunked(2).forEach { row ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            row.forEach { (artTitle, stats, series) ->
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                                                    border = BorderStroke(1.dp, Color(0xFF333333)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Column {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(130.dp)
                                                                .background(Brush.linearGradient(listOf(Color(0xFF8A2BE2), Color(0xFF00E5FF))))
                                                        )
                                                        Column(modifier = Modifier.padding(8.dp)) {
                                                            Text(artTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                            Text(series, color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                            Text(stats, color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Media" -> {
                        if (mediaItems.itemCount == 0 && mediaItems.loadState.refresh is LoadState.NotLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Text("No media items yet", color = AppTextSecondary)
                                }
                            }
                        } else {
                            items(
                                count = mediaItems.itemCount,
                                key = mediaItems.itemKey { it.id }
                            ) { index ->
                                val post = mediaItems[index]
                                if (post != null) {
                                    PostItem(
                                        adManager = adManager,
                                        post = post,
                                        isOwnPost = post.authorId == currentUser.id || (currentUser.username.length > 0 && post.authorHandle == currentUser.username),
                                        activePostId = activePostId,
                                        onClick = { onNavigateToPostDetail(post.id) },
                                        onQuotedPostClick = { quotedPost -> onNavigateToPostDetail(quotedPost.id) },
                                        onLike = { id, likes, liked -> mediaViewModel.likePost(id, likes, liked) },
                                        onRepostClick = { p -> mediaViewModel.repostPost(p.id, p.repostCount, p.isReposted) },
                                        onBookmark = { mediaViewModel.bookmarkPost(it) },
                                        onDelete = { mediaViewModel.deletePost(post.id) },
                                        onMoreClick = { selectedPostForMore = it },
                                        onReplyClick = { p, r -> onNavigateToPostDetail(p.id) },
                                        onProfileClick = { targetId ->
                                            if (targetId.isNotBlank() && targetId != "null" && targetId != userId) {
                                                com.tpsk.tj.common.util.GlobalNavigation.navigateTo(
                                                    com.tpsk.tj.ui.navigation.Screen.UserProfile.createRoute(targetId)
                                                )
                                            }
                                        },
                                        onHashtagClick = { tag ->
                                            val clean = tag.removePrefix("#").trim()
                                            if (clean.isNotBlank()) {
                                                com.tpsk.tj.common.util.GlobalNavigation.navigateTo(
                                                    com.tpsk.tj.media.ui.components.hashtagRoute(clean)
                                                )
                                            }
                                        },
                                        onPlaySong = { _, _ -> },
                                        onVotePoll = { pid, oid -> mediaViewModel.votePoll(pid, oid) },
                                        onMediaClick = { url, type, pos -> onNavigateToMediaViewer(url, type, pos) },
                                        onShareClick = { p -> 
                                            mediaViewModel.sharePost(p.id, p.shares)
                                            com.tpsk.tj.common.util.ShareUtils.sharePost(context, p) 
                                        }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                    "Replies" -> {
                        if (replies.itemCount == 0 && replies.loadState.refresh is LoadState.NotLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Text("No replies yet", color = AppTextSecondary)
                                }
                            }
                        } else {
                            items(
                                count = replies.itemCount,
                                key = replies.itemKey { it.id }
                            ) { index ->
                                val post = replies[index]
                                if (post != null) {
                                    PostItem(
                                        adManager = adManager,
                                        post = post,
                                        isOwnPost = post.authorId == currentUser.id || (currentUser.username.length > 0 && post.authorHandle == currentUser.username),
                                        activePostId = activePostId,
                                        onClick = { onNavigateToPostDetail(post.id) },
                                        onQuotedPostClick = { quotedPost -> onNavigateToPostDetail(quotedPost.id) },
                                        onLike = { id, likes, liked -> mediaViewModel.likePost(id, likes, liked) },
                                        onRepostClick = { p -> mediaViewModel.repostPost(p.id, p.repostCount, p.isReposted) },
                                        onBookmark = { mediaViewModel.bookmarkPost(it) },
                                        onDelete = { mediaViewModel.deletePost(post.id) },
                                        onMoreClick = { selectedPostForMore = it },
                                        onReplyClick = { p, r -> onNavigateToPostDetail(p.id) },
                                        onProfileClick = { targetId ->
                                            if (targetId.isNotBlank() && targetId != "null" && targetId != userId) {
                                                com.tpsk.tj.common.util.GlobalNavigation.navigateTo(
                                                    com.tpsk.tj.ui.navigation.Screen.UserProfile.createRoute(targetId)
                                                )
                                            }
                                        },
                                        onHashtagClick = { tag ->
                                            val clean = tag.removePrefix("#").trim()
                                            if (clean.isNotBlank()) {
                                                com.tpsk.tj.common.util.GlobalNavigation.navigateTo(
                                                    com.tpsk.tj.media.ui.components.hashtagRoute(clean)
                                                )
                                            }
                                        },
                                        onPlaySong = { _, _ -> },
                                        onVotePoll = { pid, oid -> mediaViewModel.votePoll(pid, oid) },
                                        onMediaClick = { url, type, pos -> onNavigateToMediaViewer(url, type, pos) },
                                        onShareClick = { p -> 
                                            mediaViewModel.sharePost(p.id, p.shares)
                                            com.tpsk.tj.common.util.ShareUtils.sharePost(context, p) 
                                        }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                    "Shorts" -> {
                        if (shorts.itemCount == 0 && shorts.loadState.refresh is LoadState.NotLoading) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Text("No shorts yet", color = AppTextSecondary)
                                }
                            }
                        } else {
                            val rowCount = (shorts.itemCount + 2) / 3
                            items(rowCount) { rowIndex ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (colIndex in 0..2) {
                                        val index = rowIndex * 3 + colIndex
                                        if (index < shorts.itemCount) {
                                            val short = shorts[index]
                                            if (short != null) {
                                                Box(modifier = Modifier.weight(1f).padding(1.dp)) {
                                                    PostItem(
                                                        adManager = adManager,
                                                        post = short,
                                                        displayMode = com.tpsk.tj.media.ui.components.PostDisplayMode.GRID,
                                                        onClick = { onShortClick(short) }
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTabItems.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppAccent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (selectedPostForMore != null) {
        val target = selectedPostForMore!!
        val isOwnPost = target.isOwnedBy(currentUser.id, currentUser.username)

        PostOptionsMenu(
            post = target,
            isOwner = isOwnPost,
            onReport = {
                reportingPost = target
                selectedPostForMore = null
            },
            onBlockUser = {
                userViewModel.blockUser(target.authorId)
                mediaViewModel.hidePost(target.id)
                Toast.makeText(context, "Blocked @${target.authorHandle}", Toast.LENGTH_SHORT).show()
                selectedPostForMore = null
            },
            onNotInterested = {
                mediaViewModel.hidePost(target.id)
                Toast.makeText(context, "We'll show fewer posts like this", Toast.LENGTH_SHORT).show()
                selectedPostForMore = null
            },
            onDelete = {
                mediaViewModel.deletePost(target.id, currentUser.id, currentUser.username)
                selectedPostForMore = null
            },
            onTurnOffComments = {
                mediaViewModel.toggleCommentsLock(target.id, target.repliesLocked)
                selectedPostForMore = null
            },
            onShare = {
                mediaViewModel.sharePost(target.id, target.shares)
                com.tpsk.tj.common.util.ShareUtils.sharePost(context, target)
                selectedPostForMore = null
            },
            onCopyLink = {
                com.tpsk.tj.common.util.ShareUtils.copyPostLink(context, target)
                selectedPostForMore = null
            },
            onBookmark = {
                mediaViewModel.bookmarkPost(target.id)
                selectedPostForMore = null
            },
            onDismiss = { selectedPostForMore = null }
        )
    }

    if (reportingPost != null) {
        val target = reportingPost!!
        ReportContentDialog(
            post = target,
            onDismiss = { reportingPost = null },
            onSubmitReport = { category, details ->
                mediaViewModel.reportPost(target.id, "$category: $details")
                Toast.makeText(context, "Report submitted. Thank you for keeping our community safe.", Toast.LENGTH_LONG).show()
                reportingPost = null
            }
        )
    }
}

@Composable
fun SuggestedUserCard(
    contact: Contact,
    onDismiss: () -> Unit = {},
    onFollow: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(210.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(4.dp)
            ) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
            
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = MediaUtils.formatUrl(contact.avatarUrl),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = contact.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "@${contact.username}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (!contact.isFollowing) {
                    Button(
                        onClick = onFollow,
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary, 
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Follow", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else {
                    // Show a disabled "Following" or nothing.
                    // Following the user's request to "not show following", we show nothing.
                    Box(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
