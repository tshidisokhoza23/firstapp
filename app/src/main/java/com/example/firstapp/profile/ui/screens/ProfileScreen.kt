package com.example.firstapp.profile.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.firstapp.common.model.*
import com.example.firstapp.media.model.Post
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.media.ui.viewmodel.SharedMediaViewModel
import com.example.firstapp.common.util.MediaUtils
import com.example.firstapp.media.ui.screens.PostItem
import com.example.firstapp.common.util.Resource
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.paging.LoadState
import com.example.firstapp.music.model.Song
import com.example.firstapp.profile.model.WorkExperience
import java.util.ArrayList
import kotlin.collections.emptyList
import kotlin.collections.forEachIndexed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(
    userId: String? = null,
    isGroup: Boolean = false,
    showExperienceTab: Boolean = true,
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
    onNavigateToEditExperience: (String?) -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onShortClick: (Post) -> Unit = {},
    onNavigateToMediaViewer: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val isLoggedIn by TokenManager.authState.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val normalizedUserId = if (userId != null && userId.isNotEmpty()) userId else null
    val isMyProfile = normalizedUserId == null || normalizedUserId == currentUser?.id
    var remoteUser by remember(normalizedUserId, isMyProfile) { mutableStateOf<User?>(null) }
    var profileRefreshNonce by remember(normalizedUserId, isMyProfile) { mutableIntStateOf(0) }
    var isFollowUpdating by remember(userId) { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn, currentUser?.id) {
        if (isLoggedIn && currentUser == null) {
            userViewModel.fetchCurrentUser()
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            onRequireLogin()
        }
    }
    
    LaunchedEffect(isLoggedIn, normalizedUserId, isMyProfile, profileRefreshNonce) {
        if (!isLoggedIn) {
            remoteUser = null
        } else if (!isMyProfile && normalizedUserId != null) {
            userViewModel.getUserById(normalizedUserId).collect { resource ->
                if (resource is Resource.Success) {
                    remoteUser = resource.data
                }
            }
        } else {
            remoteUser = null
        }
    }

    val user = if (isMyProfile) currentUser else remoteUser

    if (!isLoggedIn) {
        return
    }

    var selectedTabIndex by remember(normalizedUserId, showExperienceTab) { mutableIntStateOf(0) }
    val tabs = remember(showExperienceTab) {
        val l = ArrayList<String>()
        l.add("Posts")
        if (showExperienceTab) l.add("Experience")
        l.add("Replies")
        l.add("Media")
        l.add("Shorts")
        l
    }
    
    val safeSelectedTabIndex = if (selectedTabIndex < tabs.size) selectedTabIndex else 0
    val selectedTab = if (safeSelectedTabIndex < tabs.size) tabs.get(safeSelectedTabIndex) else if (tabs.size > 0) tabs.get(tabs.size - 1) else ""
    val profileOwnerId = normalizedUserId ?: currentUser?.id

    val workExperiencesResource by userViewModel.workExperiences.collectAsState()
    val workExperiences = if (workExperiencesResource is Resource.Success) {
        val data = (workExperiencesResource as Resource.Success<List<WorkExperience>>).data
        if (data != null) data else emptyList<WorkExperience>()
    } else {
        emptyList<WorkExperience>()
    }

    val isRefreshing = posts.loadState.refresh is LoadState.Loading

    Scaffold(
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { posts.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (user != null) {
                                    val nameToShow = if (user.displayName != null && user.displayName.isNotEmpty()) user.displayName else "No Name"
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

                            AsyncImage(
                                model = user?.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(AppSecondary)
                                    .clickable { 
                                        val url = user?.avatarUrl
                                        if (url != null) {
                                            onNavigateToMediaViewer(url, "image")
                                        }
                                    },
                                contentScale = ContentScale.Crop
                            )
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
                            Text(
                                text = "${user?.followersCount ?: 0}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Followers",
                                color = AppTextSecondary,
                                fontSize = 14.sp
                            )
                            
                            Box(modifier = Modifier.size(2.dp).clip(CircleShape).background(AppTextSecondary))

                            Text(
                                text = "0",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Views",
                                color = AppTextSecondary,
                                fontSize = 14.sp
                            )

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
                                    Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                Text(if (isMyProfile) "Share Profile" else "Message", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                        tabs.forEachIndexed { idx, title ->
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
                        items(
                            count = posts.itemCount,
                            key = posts.itemKey { it?.id ?: 0 }
                        ) { index ->
                            val post = posts[index]
                            if (post != null) {
                                PostItem(
                                    adManager = adManager,
                                    post = post,
                                    isOwnPost = post.authorId == currentUser?.id,
                                    onClick = { onNavigateToMediaViewer(post.id, "post") },
                                    onLike = { mediaViewModel.likePost(post.id, post.likes, post.isLiked) },
                                    onRepostClick = { /* mediaViewModel.repostPost(post.id) */ },
                                    onBookmark = { mediaViewModel.bookmarkPost(post.id) },
                                    onDelete = { mediaViewModel.deletePost(post.id) },
                                    onReplyClick = { onNavigateToMediaViewer(post.id, "post") },
                                    onProfileClick = {},
                                    onPlaySong = { _, _ -> },
                                    onVotePoll = { _ -> }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                    "Experience" -> {
                        if (workExperiences.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                                    Text("No work experience added", color = AppTextSecondary)
                                }
                            }
                        } else {
                            items(workExperiences) { experience ->
                                WorkExperienceItem(
                                    experience = experience,
                                    isMyProfile = isMyProfile,
                                    onEdit = { onNavigateToEditExperience(experience.id) }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                    "Media" -> {
                        items(
                            count = mediaItems.itemCount,
                            key = mediaItems.itemKey { it.id }
                        ) { index ->
                            val post = mediaItems[index]
                            if (post != null) {
                                PostItem(
                                    adManager = adManager,
                                    post = post,
                                    isOwnPost = post.authorId == currentUser?.id,
                                    onClick = { onNavigateToMediaViewer(post.id, "post") },
                                    onLike = { mediaViewModel.likePost(post.id, post.likes, post.isLiked) },
                                    onRepostClick = { /* showRepostOptions = it */ },
                                    onBookmark = { mediaViewModel.bookmarkPost(post.id) },
                                    onDelete = { mediaViewModel.deletePost(post.id) },
                                    onReplyClick = { onNavigateToMediaViewer(post.id, "post") },
                                    onProfileClick = {},
                                    onPlaySong = { _, _ -> },
                                    onVotePoll = { _ -> }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                    "Replies" -> {
                        items(
                            count = replies.itemCount,
                            key = replies.itemKey { it.id }
                        ) { index ->
                            val post = replies[index]
                            if (post != null) {
                                PostItem(
                                    adManager = adManager,
                                    post = post,
                                    isOwnPost = post.authorId == currentUser?.id,
                                    onClick = { onNavigateToMediaViewer(post.id, "post") },
                                    onLike = { mediaViewModel.likePost(post.id, post.likes, post.isLiked) },
                                    onRepostClick = { /* showRepostOptions = it */ },
                                    onBookmark = { mediaViewModel.bookmarkPost(post.id) },
                                    onDelete = { mediaViewModel.deletePost(post.id) },
                                    onReplyClick = { onNavigateToMediaViewer(post.id, "post") },
                                    onProfileClick = {},
                                    onPlaySong = { _, _ -> },
                                    onVotePoll = { _ -> }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            }
                        }
                    }
                    "Shorts" -> {
                        val rowCount = (shorts.itemCount + 2) / 3
                        items(rowCount) { rowIndex ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (colIndex in 0 until 3) {
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

                if (posts.loadState.append is LoadState.Loading) {
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
private fun WorkExperienceItem(
    experience: WorkExperience,
    isMyProfile: Boolean,
    onEdit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(experience.jobTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(experience.companyName, color = AppAccent, fontSize = 16.sp)
            }
            if (isMyProfile) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
        
        if (experience.location != null) {
            Text(experience.location!!, color = AppTextSecondary, fontSize = 14.sp)
        }
        
        Text(
            "${experience.employmentType} • ${if (experience.isCurrentJob) "Present" else "Past"}",
            color = AppTextSecondary,
            fontSize = 12.sp
        )
        
        if (experience.description != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(experience.description!!, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
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
        val model = if (post.thumbnailUrl != null && post.thumbnailUrl.isNotEmpty()) post.thumbnailUrl else if (post.mediaUrls.isNotEmpty()) post.mediaUrls[0] else null
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
