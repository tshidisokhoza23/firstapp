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
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.common.util.Resource
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.paging.LoadState
import com.example.firstapp.music.model.Song
import com.example.firstapp.profile.model.WorkExperience
import kotlin.collections.*
import kotlin.text.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(
    userId: String? = null,
    isGroup: Boolean = false,
    showExperienceTab: Boolean = true,
    onBack: () -> Unit,
    posts: LazyPagingItems<Post>,
    userViewModel: UserViewModel,
    mediaViewModel: MediaViewModel,
    sharedMediaViewModel: SharedMediaViewModel = hiltViewModel(),
    shortsViewModel: ShortsViewModel = hiltViewModel(),
    onNavigateToSharedMedia: (String) -> Unit = {},
    onRequireLogin: () -> Unit = {},
    onNavigateToEditExperience: (String?) -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onShortClick: (ShortMedia) -> Unit = {},
    onNavigateToMediaViewer: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val isLoggedIn by TokenManager.authState.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val normalizedUserId = if (userId != null && userId.length > 0) userId else null
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
        val l = java.util.ArrayList<String>()
        l.add("Posts")
        if (showExperienceTab) l.add("Experience")
        l.add("Media")
        l.add("Replies")
        l.add("Shorts")
        l
    }
    
    val safeSelectedTabIndex = if (selectedTabIndex < tabs.size) selectedTabIndex else 0
    val selectedTab = if (safeSelectedTabIndex < tabs.size) tabs[safeSelectedTabIndex] else if (tabs.size > 0) tabs[tabs.size - 1] else ""
    val profileOwnerId = normalizedUserId ?: currentUser?.id

    val workExperiencesResource by userViewModel.workExperiences.collectAsState()
    val workExperiences = if (workExperiencesResource is Resource.Success) {
        val data = (workExperiencesResource as Resource.Success<List<WorkExperience>>).data
        if (data != null) data else java.util.Collections.emptyList<WorkExperience>()
    } else {
        java.util.Collections.emptyList<WorkExperience>()
    }

    val userShortsState by shortsViewModel.userShorts.collectAsState()
    val shortsData = userShortsState.data ?: emptyList()
    val shorts = remember(shortsData, profileOwnerId) {
        val filtered = java.util.ArrayList<ShortMedia>()
        for (short in shortsData) {
            if (profileOwnerId == null || short.creatorId == profileOwnerId) {
                filtered.add(short)
            }
        }
        filtered
    }
    
    val chunkedShorts = remember(shorts) {
        val result = java.util.ArrayList<List<ShortMedia>>()
        var i = 0
        while (i < shorts.size) {
            val end = if (i + 3 > shorts.size) shorts.size else i + 3
            result.add(shorts.subList(i, end))
            i += 3
        }
        result
    }

    LaunchedEffect(isLoggedIn, normalizedUserId, currentUser?.id) {
        val targetId = normalizedUserId ?: currentUser?.id
        if (isLoggedIn && targetId != null) {
            shortsViewModel.setSelectedShortsUserId(targetId)
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val file = MediaUtils.getFileFromUri(context, uri)
            if (file != null) userViewModel.uploadProfilePicture(file, isBanner = false)
        }
    }

    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val file = MediaUtils.getFileFromUri(context, uri)
            if (file != null) userViewModel.uploadProfilePicture(file, isBanner = true)
        }
    }

    val isRefreshing = posts.loadState.refresh is LoadState.Loading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isGroup) "Group Info" else if (isMyProfile) "My Profile" else "Profile", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isMyProfile) {
                        IconButton(onClick = onNavigateToEditProfile) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppAccent)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { posts.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(if (user?.bannerUrl == null) 120.dp else 180.dp)) {
                        AsyncImage(
                            model = user?.bannerUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().background(AppSecondary).clickable { 
                                val url = user?.bannerUrl
                                if (url != null) {
                                    onNavigateToMediaViewer(url, "image")
                                }
                            },
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, AppBackground.copy(alpha = 0.8f)))))
                        
                        if (isMyProfile) {
                            IconButton(
                                onClick = { bannerLauncher.launch("image/*") },
                                modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Banner", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-40).dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box {
                                AsyncImage(
                                    model = user?.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, AppBackground, CircleShape)
                                        .background(AppSecondary)
                                        .clickable { 
                                            val url = user?.avatarUrl
                                            if (url != null) {
                                                onNavigateToMediaViewer(url, "image")
                                            }
                                        },
                                    contentScale = ContentScale.Crop
                                )
                                if (isMyProfile) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(CircleShape)
                                            .background(AppAccent)
                                            .clickable { imageLauncher.launch("image/*") },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.padding(bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isMyProfile) {
                                    Button(
                                        onClick = onNavigateToEditProfile,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isFollowing) Color.Transparent else Color.White,
                                            contentColor = if (isFollowing) Color.White else Color.Black
                                        ),
                                        border = if (isFollowing) BorderStroke(1.dp, Color.Gray) else null,
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                
                                FilledIconButton(
                                    onClick = { 
                                        if (isMyProfile) {
                                            onNavigateToSharedMedia("inbox")
                                        } else if (user != null) {
                                            onNavigateToMediaViewer(user.id, "chat")
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = AppSecondary.copy(alpha = 0.9f)
                                    )
                                ) {
                                    Icon(Icons.Default.Mail, contentDescription = "Inbox", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (user != null) {
                            val nameToShow = if (user.displayName != null && user.displayName.length > 0) user.displayName else "No Name"
                            Text(nameToShow, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                            Text("@${user.username}", color = AppTextSecondary, fontSize = 15.sp)
                        } else {
                            Box(modifier = Modifier.width(150.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
                        }

                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user?.followingCount?.toString() ?: "0", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Text("Following", color = AppTextSecondary, fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(user?.followersCount?.toString() ?: "0", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Text("Followers", color = AppTextSecondary, fontSize = 14.sp)
                            }
                        }

                        if (user?.bio?.isNotBlank() == true) {
                            Text(user.bio, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        
                        if (user?.location?.isNotBlank() == true) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                                Icon(Icons.Default.LocationOn, null, tint = AppTextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(user.location, color = AppTextSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }

                item {
                    TabRow(
                        selectedTabIndex = safeSelectedTabIndex,
                        containerColor = AppBackground,
                        contentColor = AppAccent,
                        divider = {},
                        indicator = { tabPositions ->
                            if (safeSelectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[safeSelectedTabIndex]),
                                    color = AppAccent
                                )
                            }
                        }
                    ) {
                        for (idx in 0..(tabs.size - 1)) {
                            val title = tabs.get(idx)
                            Tab(
                                selected = safeSelectedTabIndex == idx,
                                onClick = { selectedTabIndex = idx },
                                text = { Text(title, fontSize = 14.sp, fontWeight = if (safeSelectedTabIndex == idx) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                when (selectedTab) {
                    "Posts" -> {
                        items(
                            count = posts.itemCount,
                            key = posts.itemKey { it.id }
                        ) { index ->
                            val post = posts[index]
                            if (post != null) {
                                PostItem(
                                    post = post,
                                    onClick = {},
                                    onLike = { mediaViewModel.likePost(post.id, post.likes, post.isLiked) },
                                    onRepost = { mediaViewModel.repostPost(post.id) },
                                    onBookmark = { mediaViewModel.bookmarkPost(post.id) },
                                    onDelete = { mediaViewModel.deletePost(post.id) },
                                    onReplyClick = { },
                                    onProfileClick = {},
                                    onPlaySong = { _, _ -> },
                                    onVotePoll = { _ -> }
                                )
                                HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
                            }
                        }
                    }
                    "Experience" -> {
                        if (workExperiences.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
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
                                HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
                            }
                        }
                    }
                    "Media" -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Media Grid coming soon", color = AppTextSecondary)
                            }
                        }
                    }
                    "Replies" -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("User Replies coming soon", color = AppTextSecondary)
                            }
                        }
                    }
                    "Shorts" -> {
                        items(chunkedShorts.size) { rowIndex ->
                            val row = chunkedShorts[rowIndex]
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (short in row) {
                                    Box(modifier = Modifier.weight(1f).padding(1.dp)) {
                                        ShortGridItem(short, onShortClick)
                                    }
                                }
                                val remaining = 3 - row.size
                                var i = 0
                                while (i < remaining) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    i++
                                }
                            }
                        }
                    }
                }

                if (posts.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppAccent)
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
            Text(experience.location, color = AppTextSecondary, fontSize = 14.sp)
        }
        
        Text(
            "${experience.employmentType} • ${if (experience.isCurrentJob) "Present" else "Past"}",
            color = AppTextSecondary,
            fontSize = 12.sp
        )
        
        if (experience.description != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(experience.description, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
        }
    }
}

@Composable
private fun ShortGridItem(short: ShortMedia, onShortClick: (ShortMedia) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(8.dp))
            .background(short.thumbnailColor)
            .clickable { onShortClick(short) }
    ) {
        AsyncImage(
            model = short.thumbnailUrl,
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
    }
}
