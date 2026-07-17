package com.example.firstapp.profile.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.media.ui.viewmodel.SharedMediaViewModel
import com.example.firstapp.common.util.MediaUtils
import com.example.firstapp.media.ui.screens.PostItem
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.media.ui.screens.shorts.ShortGridItem
import com.example.firstapp.common.util.Resource
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.paging.LoadState
import java.util.*
import java.text.SimpleDateFormat
import kotlin.collections.*
import kotlin.text.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(
    userId: String? = null,
    isGroup: Boolean = false,
    onBack: () -> Unit,
    posts: LazyPagingItems<Post>,
    userViewModel: UserViewModel,
    mediaViewModel: MediaViewModel,
    chatViewModel: ChatViewModel = hiltViewModel(),
    sharedMediaViewModel: SharedMediaViewModel = hiltViewModel(),
    shortsViewModel: ShortsViewModel = hiltViewModel(),
    onNavigateToSharedMedia: (String) -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToEditExperience: (String?) -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onShortClick: (ShortMedia) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUser by userViewModel.currentUser.collectAsState()
    val isMyProfile = userId.isNullOrBlank() || userId == currentUser?.id
    var remoteUser by remember(userId, isMyProfile) { mutableStateOf<User?>(null) }
    
    val contacts by userViewModel.contacts.collectAsState()
    val chatsResource by chatViewModel.chats.collectAsState()
    val chatList = (chatsResource.data ?: emptyList<com.example.firstapp.chat.model.ChatMessage>()) as List<com.example.firstapp.chat.model.ChatMessage>
    val chat = chatList.find { it.id == userId }
    
    LaunchedEffect(userId, isMyProfile) {
        if (!isMyProfile && !userId.isNullOrBlank()) {
            userViewModel.getUserById(userId).collect { resource ->
                if (resource is Resource.Success) {
                    remoteUser = resource.data
                }
            }
        } else {
            remoteUser = null
        }
    }

    val user = if (isMyProfile) currentUser else {
        if (remoteUser != null) {
            remoteUser
        } else {
        val contact = contacts.find { it.id == userId }
        if (contact != null) {
            User(
                id = contact.id, 
                username = contact.username, 
                displayName = chat?.senderName ?: contact.displayName, 
                avatarUrl = contact.avatarUrl, 
                bio = contact.bio, 
                location = "",
                phoneNumber = contact.phoneNumber
            )
        } else if (chat != null) {
            User(
                id = chat.id,
                displayName = chat.senderName,
                avatarUrl = chat.avatarUrl,
                username = ""
            )
        } else null
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = arrayOf("Posts", "Experience", "Media", "Replies", "Shorts")

    val workExperiencesResource by userViewModel.workExperiences.collectAsState()
    val workExperiences = if (workExperiencesResource is Resource.Success) {
        (workExperiencesResource as Resource.Success<List<com.example.firstapp.profile.model.WorkExperience>>).data ?: emptyList()
    } else emptyList()

    val sortedExperiences = remember(workExperiences) {
        workExperiences.sortedWith(compareByDescending<com.example.firstapp.profile.model.WorkExperience> { it.isCurrentJob }
            .thenByDescending { it.startDate })
    }

    val userShortsState by shortsViewModel.shorts.collectAsState()
    val shorts = userShortsState.data ?: emptyList<ShortMedia>()

    LaunchedEffect(userId) {
        if (userId != null) {
            shortsViewModel.getUserShorts(userId)
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = MediaUtils.getFileFromUri(context, it)
            if (file != null) userViewModel.uploadProfilePicture(file, isBanner = false)
        }
    }

    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = MediaUtils.getFileFromUri(context, it)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        AsyncImage(
                            model = user?.bannerUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().background(AppSecondary).clickable(enabled = isMyProfile) { bannerLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(kotlin.collections.listOf(Color.Transparent, AppBackground))))
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-36).dp)) {
                        AsyncImage(
                            model = user?.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp).clip(CircleShape).border(4.dp, AppBackground, CircleShape).background(AppSecondary).clickable(enabled = isMyProfile) { imageLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (user != null) {
                            val nameToShow = if (user.displayName.isNotBlank()) user.displayName else "No Name"
                            Text(nameToShow, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("@${user.username}", color = AppTextSecondary, fontSize = 16.sp)
                        } else {
                            // Skeleton/Loading state
                            Box(modifier = Modifier.width(150.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp)))
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.width(100.dp).height(16.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp)))
                        }
                        
                        if (!isMyProfile && user != null) {
                            Row(modifier = Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onNavigateToChat(user.id) }, colors = ButtonDefaults.buttonColors(containerColor = AppAccent), modifier = Modifier.weight(1f)) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Message")
                                }
                                OutlinedButton(onClick = { /* Call */ }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Call")
                                }
                            }
                        }

                    }
                }

                item {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = AppBackground,
                        contentColor = AppAccent,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = AppAccent
                                )
                            }
                        }
                    ) {
                        var k = 0
                        while (k < tabs.size) {
                            val title = tabs[k]
                            val idx = k
                            Tab(
                                selected = selectedTabIndex == idx,
                                onClick = { selectedTabIndex = idx },
                                text = { Text(title, fontSize = 14.sp, fontWeight = if (selectedTabIndex == idx) FontWeight.Bold else FontWeight.Normal) }
                            )
                            k++
                        }
                    }
                }

                when (selectedTabIndex) {
                    0 -> { // Posts
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
                    1 -> { // Experience
                        item {
                            if (isMyProfile) {
                                Button(
                                    onClick = { onNavigateToEditExperience(null) },
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add Experience")
                                }
                            }
                        }
                        items(sortedExperiences.size) { index ->
                            val exp = sortedExperiences[index]
                            WorkExperienceItem(
                                experience = exp,
                                isMyProfile = isMyProfile,
                                onEditClick = { onNavigateToEditExperience(exp.id) }
                            )
                            HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
                        }
                        if (sortedExperiences.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No work experience added", color = AppTextSecondary)
                                }
                            }
                        }
                    }
                    2 -> { // Media
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Media Grid coming soon", color = AppTextSecondary)
                            }
                        }
                    }
                    3 -> { // Replies
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("User Replies coming soon", color = AppTextSecondary)
                            }
                        }
                    }
                    4 -> { // Shorts
                        val chunkedShorts = ArrayList<List<ShortMedia>>()
                        var i = 0
                        while (i < shorts.size) {
                            val chunk = ArrayList<ShortMedia>()
                            var j = 0
                            while (j < 3 && (i + j) < shorts.size) {
                                chunk.add(shorts[i + j])
                                j++
                            }
                            chunkedShorts.add(chunk)
                            i += 3
                        }
                        
                        items(chunkedShorts.size) { rowIndex ->
                            val row = chunkedShorts[rowIndex]
                            Row(modifier = Modifier.fillMaxWidth()) {
                                var m = 0
                                while (m < row.size) {
                                    val short = row[m]
                                    Box(modifier = Modifier.weight(1f).padding(1.dp)) {
                                        ShortGridItem(short, onShortClick)
                                    }
                                    m++
                                }
                                val remaining = 3 - row.size
                                if (remaining > 0) {
                                    var n = 0
                                    while (n < remaining) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        n++
                                    }
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
fun ProfileOptionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color = Color.White, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = color) },
        leadingContent = { Icon(icon, null, tint = if (color == Color.Red) color else AppAccent) },
        modifier = Modifier.clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun WorkExperienceItem(
    experience: com.example.firstapp.profile.model.WorkExperience,
    isMyProfile: Boolean,
    onEditClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    
    ListItem(
        overlineContent = {
            Text(
                text = "${dateFormat.format(Date(experience.startDate))} - ${if (experience.isCurrentJob) "Present" else experience.endDate?.let { dateFormat.format(Date(it)) } ?: ""}",
                color = AppTextSecondary,
                fontSize = 12.sp
            )
        },
        headlineContent = {
            Text(experience.jobTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        supportingContent = {
            Column {
                Text("${experience.companyName} • ${experience.employmentType}", color = AppAccent, fontWeight = FontWeight.Medium)
                if (!experience.location.isNullOrBlank()) {
                    Text(experience.location ?: "", color = AppTextSecondary, fontSize = 14.sp)
                }
                if (!experience.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(experience.description ?: "", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }
        },
        trailingContent = {
            if (isMyProfile) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppAccent, modifier = Modifier.size(20.dp))
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
