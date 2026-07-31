@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.firstapp.profile.ui.screens

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
import com.example.firstapp.common.model.*
import com.example.firstapp.media.model.Post
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.media.ui.viewmodel.SharedMediaViewModel
import com.example.firstapp.common.util.MediaUtils
import com.example.firstapp.media.ui.components.PostItem
import com.example.firstapp.common.util.Resource
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.paging.LoadState
import com.example.firstapp.chat.model.Contact
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
    adManager: com.example.firstapp.common.data.ads.AdManager,
    sharedMediaViewModel: SharedMediaViewModel = hiltViewModel(),
    onNavigateToSharedMedia: (String) -> Unit = {},
    onRequireLogin: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onShortClick: (Post) -> Unit = {},
    onNavigateToMediaViewer: (String, String) -> Unit = { _, _ -> },
    onNavigateToPostDetail: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isLoggedIn by userViewModel.isLoggedIn.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val normalizedUserId = remember(userId) { if (userId.length > 0) userId else null }
    val isMyProfile = remember(normalizedUserId, currentUser?.id) { 
        normalizedUserId == null || normalizedUserId == currentUser?.id 
    }
    
    var remoteUser by remember(normalizedUserId) { mutableStateOf<User?>(null) }
    var profileRefreshNonce by remember(normalizedUserId) { mutableIntStateOf(0) }
    var isFollowUpdating by remember(userId) { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && currentUser == null) {
            userViewModel.fetchCurrentUser()
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            onRequireLogin()
        }
    }
    
    LaunchedEffect(isLoggedIn, normalizedUserId, profileRefreshNonce) {
        if (!isLoggedIn) {
            remoteUser = null
        } else if (normalizedUserId != null) {
            userViewModel.getUserById(normalizedUserId).collect { resource ->
                if (resource is Resource.Success) {
                    remoteUser = resource.data
                }
            }
        }
    }

    val user = remember(isMyProfile, currentUser, remoteUser) {
        if (isMyProfile) {
            currentUser ?: remoteUser
        } else {
            remoteUser
        }
    }

    if (!isLoggedIn) {
        return
    }

    var selectedTabIndex by remember(normalizedUserId) { mutableIntStateOf(0) }
    val tabs = remember {
        val l = ArrayList<String>()
        l.add("Posts")
        l.add("Replies")
        l.add("Media")
        l.add("Shorts")
        l
    }
    
    val safeSelectedTabIndex = if (selectedTabIndex < tabs.size) selectedTabIndex else 0
    val selectedTab = if (safeSelectedTabIndex < tabs.size) tabs.get(safeSelectedTabIndex) else if (tabs.size > 0) tabs.get(tabs.size - 1) else ""
    val profileOwnerId = normalizedUserId ?: currentUser?.id

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
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                    }
                    IconButton(onClick = { /* TODO: Notifications */ }) {
                        Icon(Icons.Default.NotificationsNone, contentDescription = "Notifications", tint = Color.White)
                    }
                    IconButton(onClick = { /* TODO: More */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBackground),
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { selectedTabItems.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
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
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        text = "@${user.username}",
                                        color = AppTextSecondary,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {
                                    Box(modifier = Modifier.width(150.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
                                    Box(modifier = Modifier.padding(top = 4.dp).width(100.dp).height(16.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                                }
                            }

                        if (user != null && !user.avatarUrl.isNullOrBlank()) {
                             val url = user.avatarUrl!!
                             AsyncImage(
                                model = MediaUtils.formatUrl(url),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(AppSecondary)
                                    .clickable { 
                                        onNavigateToMediaViewer(url, "image")
                                    },
                                contentScale = ContentScale.Crop,
                                error = androidx.compose.ui.graphics.painter.ColorPainter(AppSecondary),
                                fallback = androidx.compose.ui.graphics.painter.ColorPainter(AppSecondary)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(AppSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, null, tint = Color.White)
                            }
                        }
                        }

                        if (user?.bio != null && user.bio!!.isNotEmpty()) {
                            Text(
                                text = user.bio!!,
                                color = Color.White,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(top = 16.dp),
                                lineHeight = 20.sp
                            )
                        }

                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${user?.followersCount ?: 0}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Followers", color = AppTextSecondary, fontSize = 14.sp)
                            Box(modifier = Modifier.size(2.dp).clip(CircleShape).background(AppTextSecondary))
                            Text("0", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Views", color = AppTextSecondary, fontSize = 14.sp)

                            if (user?.location != null && user.location!!.isNotEmpty()) {
                                Box(modifier = Modifier.size(2.dp).clip(CircleShape).background(AppTextSecondary))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = AppTextSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(user.location!!, color = AppTextSecondary, fontSize = 14.sp)
                                }
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
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
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
                                        containerColor = if (isFollowing) Color.Transparent else Color.White,
                                        contentColor = if (isFollowing) Color.White else Color.Black
                                    ),
                                    border = if (isFollowing) BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)) else null,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }

                            Button(
                                onClick = { 
                                    if (isMyProfile) {
                                        onNavigateToSharedMedia("inbox")
                                    } else if (user != null) {
                                        onNavigateToMediaViewer(user.id, "chat")
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(if (isMyProfile) "Share profile" else "Message", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        if (isMyProfile) {
                            val uiState by mediaViewModel.uiState.collectAsState()
                            val suggestedUsers = uiState.suggestedUsers
                            
                            if (suggestedUsers.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Suggested for you",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(suggestedUsers) { contact ->
                                        SuggestedUserCard(
                                            contact = contact,
                                            onFollow = { mediaViewModel.followUser(contact.id) }
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
                            containerColor = AppBackground,
                            contentColor = Color.White,
                            divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) },
                            indicator = { tabPositions ->
                                if (safeSelectedTabIndex < tabPositions.size) {
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[safeSelectedTabIndex]),
                                        color = Color.White,
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
                                            color = if (safeSelectedTabIndex == idx) Color.White else AppTextSecondary
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
                                        isOwnPost = post.authorId == currentUser?.id || (currentUser?.username?.isNotEmpty() == true && post.authorHandle == currentUser?.username),
                                        onClick = { onNavigateToPostDetail(post.id) },
                                        onLike = { id, likes, liked -> mediaViewModel.likePost(id, likes, liked) },
                                        onRepostClick = { mediaViewModel.repostPost(it.id) },
                                        onBookmark = { mediaViewModel.bookmarkPost(it) },
                                        onDelete = { mediaViewModel.deletePost(post.id) },
                                        onReplyClick = { p, r -> onNavigateToPostDetail(p.id) },
                                        onProfileClick = { targetUser ->
                                            if (targetUser.id != userId) {
                                                // Navigate if it's a different user
                                                // For now, it's a bit limited within the same view
                                            }
                                        },
                                        onPlaySong = { _, _ -> },
                                        onVotePoll = { pid, oid -> },
                                        onMediaClick = onNavigateToMediaViewer,
                                        onShareClick = { p -> com.example.firstapp.common.util.ShareUtils.sharePost(context, p) }
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
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
                                        isOwnPost = post.authorId == currentUser?.id || (currentUser?.username?.isNotEmpty() == true && post.authorHandle == currentUser?.username),
                                        onClick = { onNavigateToPostDetail(post.id) },
                                        onLike = { id, likes, liked -> mediaViewModel.likePost(id, likes, liked) },
                                        onRepostClick = { mediaViewModel.repostPost(it.id) },
                                        onBookmark = { mediaViewModel.bookmarkPost(it) },
                                        onDelete = { mediaViewModel.deletePost(post.id) },
                                        onReplyClick = { p, r -> onNavigateToPostDetail(p.id) },
                                        onProfileClick = { targetUser ->
                                            if (targetUser.id != userId) {
                                            }
                                        },
                                        onPlaySong = { _, _ -> },
                                        onVotePoll = { pid, oid -> },
                                        onMediaClick = onNavigateToMediaViewer,
                                        onShareClick = { p -> com.example.firstapp.common.util.ShareUtils.sharePost(context, p) }
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
                                        isOwnPost = post.authorId == currentUser?.id || (currentUser?.username?.isNotEmpty() == true && post.authorHandle == currentUser?.username),
                                        onClick = { onNavigateToPostDetail(post.id) },
                                        onLike = { id, likes, liked -> mediaViewModel.likePost(id, likes, liked) },
                                        onRepostClick = { mediaViewModel.repostPost(it.id) },
                                        onBookmark = { mediaViewModel.bookmarkPost(it) },
                                        onDelete = { mediaViewModel.deletePost(post.id) },
                                        onReplyClick = { p, r -> onNavigateToPostDetail(p.id) },
                                        onProfileClick = { targetUser ->
                                            if (targetUser.id != userId) {
                                            }
                                        },
                                        onPlaySong = { _, _ -> },
                                        onVotePoll = { pid, oid -> },
                                        onMediaClick = onNavigateToMediaViewer,
                                        onShareClick = { p -> com.example.firstapp.common.util.ShareUtils.sharePost(context, p) }
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
                                                    ShortGridItem(short, { onShortClick(it) })
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
}

@Composable
fun SuggestedUserCard(
    contact: Contact,
    onFollow: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(210.dp),
        colors = CardDefaults.cardColors(containerColor = AppSecondary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { /* TODO: Dismiss */ },
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp).padding(4.dp)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            }
            
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = MediaUtils.formatUrl(contact.avatarUrl),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = contact.displayName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "@${contact.username}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onFollow,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Follow", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ShortGridItem(post: Post, onShortClick: (Post) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(4.dp))
            .background(post.mediaPlaceholderColor ?: Color.Black)
            .clickable { onShortClick(post) }
    ) {
        val model = if (post.thumbnailUrl?.isNotEmpty() == true) post.thumbnailUrl else if (post.mediaUrls.isNotEmpty()) post.mediaUrls[0] else null
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                    )
                )
        )

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play short",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp)
        )
        
        Text(
            text = "${post.views}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
    }
}
