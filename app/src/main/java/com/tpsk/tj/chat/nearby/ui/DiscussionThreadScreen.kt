package com.tpsk.tj.chat.nearby.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tpsk.tj.chat.nearby.model.NearbyPost
import com.tpsk.tj.chat.nearby.model.NearbyReply
import com.tpsk.tj.chat.nearby.ui.viewmodel.NearbyViewModel
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.common.util.formatTwitterTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val AccentCyan = Color(0xFF00B4D8)
val DarkNavyHeader = Color(0xFF0F2B36)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscussionThreadScreen(
    postId: String,
    postTitle: String,
    postSubtitle: String,
    authorName: String,
    authorDistance: String,
    authorTime: String,
    postContent: String,
    authorId: String = "",
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit = {},
    nearbyViewModel: NearbyViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val uiState by nearbyViewModel.uiState.collectAsState()

    var replyText by remember { mutableStateOf("") }
    var showRulesBanner by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val currentUserName = remember(currentUser) {
        currentUser?.displayName?.ifBlank { currentUser?.username }?.ifBlank { "You" } ?: "You"
    }

    val loadedPost = uiState.activeThreadPost?.takeIf { it.id == postId }
    val shownAuthor = loadedPost?.authorName?.takeIf { it.isNotBlank() } ?: authorName
    val shownContent = loadedPost?.content?.takeIf { it.isNotBlank() } ?: postContent
    val shownAuthorId = loadedPost?.userId?.takeIf { it.isNotBlank() } ?: authorId
    val shownDistance = if (loadedPost != null && loadedPost.distanceKm > 0.0) {
        String.format(java.util.Locale.US, "%.1f km away", loadedPost.distanceKm)
    } else {
        authorDistance
    }
    val repliesList = loadedPost?.replies.orEmpty()
    val participantCount = repliesList.size + 1

    LaunchedEffect(postId) {
        while (true) {
            delay(8_000)
            nearbyViewModel.refreshOpenThread()
        }
    }

    LaunchedEffect(postId) {
        nearbyViewModel.openThread(
            NearbyPost(
                id = postId,
                userId = "",
                authorName = authorName,
                content = postContent,
                coarseAreaName = postSubtitle.substringBefore("•").trim().ifBlank { "Nearby" }
            )
        )
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (!message.isNullOrBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val resolvedTitle = remember(shownAuthor, postTitle) {
        if (postTitle.isNotBlank()) postTitle
        else "${shownAuthor.ifBlank { "Nearby" }}'s Public Thread"
    }

    val resolvedSubtitle = remember(postSubtitle, participantCount) {
        if (postSubtitle.contains("participants") || postSubtitle.contains("Comments")) postSubtitle
        else "${postSubtitle.ifBlank { "Nearby" }} • $participantCount participants"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkNavyHeader),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = "Discussion Thread",
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = resolvedTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = resolvedSubtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showRulesBanner = !showRulesBanner }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Thread Rules",
                            tint = if (showRulesBanner) AccentCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Profile") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    if (shownAuthorId.isBlank()) {
                                        Toast.makeText(context, "Profile is not available for this post", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onOpenProfile(shownAuthorId)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Thread") },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    nearbyViewModel.reportPost(postId)
                                    Toast.makeText(context, "Thread reported and hidden", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block Author") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    if (shownAuthorId.isBlank()) {
                                        Toast.makeText(context, "Could not identify the author", Toast.LENGTH_SHORT).show()
                                    } else {
                                        nearbyViewModel.blockUser(shownAuthorId)
                                        Toast.makeText(context, "Author blocked", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            // Sticky Bottom Input Bar (Anchored above keyboard & system insets)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime)
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Current User Avatar
                    if (!currentUser?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = com.tpsk.tj.common.util.MediaUtils.formatUrl(currentUser?.avatarUrl),
                            contentDescription = "You",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF689F38)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUserName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Text Field "Write a public reply..."
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { if (it.length <= 250) replyText = it },
                        placeholder = {
                            Text(
                                "Write a public reply...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = AccentCyan
                        )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Send Button
                    Surface(
                        shape = CircleShape,
                        color = if (replyText.trim().isNotEmpty()) AccentCyan else Color(0xFF1F3A44),
                        modifier = Modifier
                            .size(42.dp)
                            .clickable(enabled = replyText.trim().isNotEmpty()) {
                                val trimmed = replyText.trim()
                                if (trimmed.isNotEmpty()) {
                                    nearbyViewModel.replyToPost(
                                        postId = postId,
                                        replyContent = trimmed,
                                        authorId = currentUser?.id.orEmpty(),
                                        authorName = currentUserName,
                                        authorAvatarUrl = currentUser?.avatarUrl,
                                        onSuccess = {
                                            replyText = ""
                                            coroutineScope.launch {
                                                if (repliesList.isNotEmpty()) {
                                                    listState.animateScrollToItem(repliesList.lastIndex)
                                                }
                                            }
                                        },
                                        onError = { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (replyText.trim().isNotEmpty()) Color.White else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Community Thread Rules Banner (if toggled)
            AnimatedVisibility(
                visible = showRulesBanner,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkNavyHeader,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Community Thread Rules",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. Public Forum: All replies are visible to everyone nearby.\n" +
                                    "2. No Inbox Flooding: Replying stays inside this public group thread and will NEVER create a private 1-on-1 chat in your inbox.\n" +
                                    "3. Respect & Safety: No harassment, spam, or hate speech. Max 250 characters.\n" +
                                    "4. Privacy First: Exact GPS coordinates are never disclosed.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Pinned Original Post Banner (Matching mockup image_9.png)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkNavyHeader,
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PINNED POST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${shownAuthor.ifBlank { "Author" }} • ${shownDistance.ifBlank { "Nearby" }} • ${authorTime.ifBlank { "now" }}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = shownContent.ifBlank { "Loading nearby post..." },
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                    val threadImages = uiState.activeThreadPost?.imageUrls?.takeIf { it.isNotEmpty() }
                        ?: uiState.posts.find { it.id == postId }?.imageUrls.orEmpty()
                    if (threadImages.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        NearbyPostImages(threadImages)
                    }
                }
            }

            // Discussion Replies List
            if (uiState.isLoading && repliesList.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentCyan)
                }
            } else if (repliesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Chat, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Be the first to reply!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Ask a question or share your thoughts with people nearby.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(repliesList, key = { it.id }) { reply ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Avatar
                            if (!reply.authorAvatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = com.tpsk.tj.common.util.MediaUtils.formatUrl(reply.authorAvatarUrl),
                                    contentDescription = reply.authorName,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(reply.authorAvatarColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = reply.authorName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Message Content
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = reply.authorName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = reply.authorAvatarColor
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = com.tpsk.tj.common.util.formatTwitterTime(reply.createdAt.toString()),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = reply.content,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
