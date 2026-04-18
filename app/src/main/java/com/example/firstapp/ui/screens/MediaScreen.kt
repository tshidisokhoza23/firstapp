package com.example.firstapp.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.model.Post
import com.example.firstapp.model.PostReply
import com.example.firstapp.model.PostType
import com.example.firstapp.ui.theme.*
import com.example.firstapp.util.formatTwitterTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

@Composable
fun MediaScreen(
    posts: List<Post>,
    mediaFeedTab: Int,
    selectedPost: Post?,
    onPostSelect: (Post?) -> Unit,
    onCreatePost: (content: String, mediaUrls: List<String>) -> Unit,
    onLikePost: (postId: Int) -> Unit,
    onReplySubmit: (postId: Int, parentReplyId: Int?, content: String) -> Unit,
    onReplyLike: (replyId: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showComposer by remember { mutableStateOf(false) }
    var composerText by remember { mutableStateOf("") }
    var selectedMedia by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var composerError by remember { mutableStateOf<String?>(null) }
    var viewerMediaUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var viewerInitialIndex by remember { mutableStateOf(0) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        selectedMedia = (selectedMedia + uris).distinct().take(10)
    }

    val currentUserId = remember { getCurrentUserIdFromToken() }

    val displayedPosts = remember(posts, mediaFeedTab, currentUserId) {
        when (mediaFeedTab) {
            0 -> {
                val mine = if (currentUserId != null) posts.filter { it.authorId == currentUserId } else emptyList()
                if (mine.isNotEmpty()) mine else posts
            }

            2 -> posts.sortedByDescending { parseEngagement(it.likes) + parseEngagement(it.views) }
            else -> posts
        }
    }

    if (selectedPost != null) {
        PostDetailContent(
            post = selectedPost,
            onBack = { onPostSelect(null) },
            onReplySubmit = { parentReplyId, content ->
                onReplySubmit(selectedPost.id, parentReplyId, content)
            },
            onReplyLike = onReplyLike,
            onMediaClick = { urls, startIndex ->
                viewerMediaUrls = urls
                viewerInitialIndex = startIndex
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (displayedPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No posts yet in this feed", color = AppTextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(displayedPosts) { post ->
                        PostItem(
                            post,
                            onClick = { onPostSelect(post) },
                            onLikeClick = { onLikePost(post.id) },
                            onMediaClick = { urls, startIndex ->
                                viewerMediaUrls = urls
                                viewerInitialIndex = startIndex
                            }
                        )
                        HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                    }
                }
            }

            FloatingActionButton(
                onClick = { showComposer = true },
                containerColor = AppAccent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post")
            }

            if (showComposer) {
                AlertDialog(
                    onDismissRequest = {
                        showComposer = false
                        composerText = ""
                        selectedMedia = emptyList()
                        composerError = null
                    },
                    containerColor = Color(0xFF101418),
                    title = { Text("Compose", color = Color.White) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = composerText,
                                onValueChange = { composerText = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                placeholder = { Text("What is happening?", color = AppTextSecondary) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppAccent,
                                    unfocusedBorderColor = AppTextSecondary.copy(alpha = 0.35f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    mediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                enabled = !isUploading,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppAccent)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add images/videos (up to 10)")
                            }

                            if (selectedMedia.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${selectedMedia.size} attachments selected",
                                    color = AppTextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(selectedMedia) { mediaUri ->
                                        Box(
                                            modifier = Modifier
                                                .size(84.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF1E2A30))
                                        ) {
                                            if (isVideoUri(context, mediaUri)) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayCircleOutline,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .align(Alignment.Center)
                                                        .size(28.dp)
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = mediaUri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            IconButton(
                                                onClick = { selectedMedia = selectedMedia - mediaUri },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(22.dp)
                                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (composerError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(composerError ?: "", color = Color(0xFFE57373), fontSize = 12.sp)
                            }

                            if (isUploading) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = AppAccent
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Uploading media...", color = AppTextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val content = composerText.trim()
                            val selectedCount = selectedMedia.size
                            if (content.isBlank() && selectedMedia.isEmpty()) {
                                composerError = "Add text or media to post"
                                return@TextButton
                            }

                            scope.launch {
                                composerError = null
                                isUploading = true
                                val uploaded = uploadComposerMedia(context, selectedMedia)
                                isUploading = false

                                if (selectedCount > 0 && uploaded.size != selectedCount) {
                                    composerError = "Uploaded ${uploaded.size}/$selectedCount files. Please retry."
                                    return@launch
                                }

                                onCreatePost(content, uploaded)
                                composerText = ""
                                selectedMedia = emptyList()
                                showComposer = false
                            }
                        }) {
                            Text("Post", color = AppAccent)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showComposer = false
                            composerText = ""
                            selectedMedia = emptyList()
                            composerError = null
                        }) {
                            Text("Cancel", color = AppTextSecondary)
                        }
                    }
                )
            }
        }
    }

    if (viewerMediaUrls.isNotEmpty()) {
        MediaViewerOverlay(
            mediaUrls = viewerMediaUrls,
            initialIndex = viewerInitialIndex,
            onClose = { viewerMediaUrls = emptyList() }
        )
    }
}

@Composable
fun PostItem(
    post: Post,
    onClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onMediaClick: (mediaUrls: List<String>, startIndex: Int) -> Unit = { _, _ -> }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        // Avatar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(post.authorAvatarColor)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.authorName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${normalizedHandle(post.authorHandle)} · ${formatTwitterTime(post.timestamp)}",
                    color = AppTextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = AppTextSecondary)
            }

            Text(
                text = post.content,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp),
                lineHeight = 20.sp
            )

            // Media Preview (supports multiple images/videos)
            if (post.mediaUrls.isNotEmpty()) {
                MediaAttachmentGrid(
                    mediaUrls = post.mediaUrls,
                    onMediaClick = { index -> onMediaClick(post.mediaUrls, index) }
                )
            } else if (post.type != PostType.TEXT_ONLY && post.mediaPlaceholderColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(post.mediaPlaceholderColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.type == PostType.VIDEO_WITH_TEXT) {
                        Icon(Icons.Default.PlayCircleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PostAction(Icons.Outlined.ChatBubbleOutline, post.repliesCount, onClick = onClick)
                PostAction(Icons.Outlined.Repeat, post.shares, tint = if (post.isReposted) AppAccent else AppTextSecondary)
                val likeIcon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
                PostAction(likeIcon, post.likes, onClick = onLikeClick)
                PostAction(Icons.Outlined.BarChart, post.views)
                Icon(Icons.Outlined.Share, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun PostAction(icon: ImageVector, count: String, tint: Color = AppTextSecondary, onClick: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }) {
        val finalTint = if (icon == Icons.Filled.Favorite) Color(0xFFE53935) else tint
        Icon(icon, contentDescription = null, tint = finalTint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(count, color = AppTextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun PostDetailContent(
    post: Post,
    onBack: () -> Unit,
    onReplySubmit: (parentReplyId: Int?, content: String) -> Unit,
    onReplyLike: (replyId: Int) -> Unit,
    onMediaClick: (mediaUrls: List<String>, startIndex: Int) -> Unit
) {
    var replyText by remember(post.id) { mutableStateOf("") }
    var replyingToId by remember(post.id) { mutableStateOf<Int?>(null) }
    var replyingToName by remember(post.id) { mutableStateOf<String?>(null) }
    var localReplies by remember(post.id) { mutableStateOf(post.replies) }
    var tempReplyIdSeed by remember(post.id) { mutableStateOf(-1) }
    val collapsedReplyIds = remember(post.id) { mutableStateMapOf<Int, Boolean>() }
    val repostedReplyIds = remember(post.id) { mutableStateMapOf<Int, Boolean>() }
    val bookmarkedReplyIds = remember(post.id) { mutableStateMapOf<Int, Boolean>() }

    LaunchedEffect(post.replies) {
        localReplies = post.replies
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Post", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = AppTextSecondary)
                }
            }
        },
        bottomBar = {
            Surface(color = AppBackground) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    if (replyingToName != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Replying to @${replyingToName}",
                                color = AppAccent,
                                fontSize = 12.sp
                            )
                            TextButton(onClick = {
                                replyingToId = null
                                replyingToName = null
                            }) {
                                Text("Cancel", color = AppTextSecondary)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Post your reply", color = AppTextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppAccent,
                                unfocusedBorderColor = AppTextSecondary.copy(alpha = 0.35f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    val content = replyText.trim()
                                    val optimistic = PostReply(
                                        id = tempReplyIdSeed,
                                        authorName = "You",
                                        authorHandle = "you",
                                        authorAvatarColor = Color(0xFF90CAF9),
                                        content = content,
                                        timestamp = "now",
                                        likes = "0",
                                        isLiked = false,
                                        nestedReplies = emptyList()
                                    )
                                    tempReplyIdSeed -= 1

                                    localReplies = if (replyingToId == null) {
                                        localReplies + optimistic
                                    } else {
                                        appendReplyToParent(localReplies, replyingToId!!, optimistic)
                                    }

                                    onReplySubmit(replyingToId, replyText.trim())
                                    replyText = ""
                                    replyingToId = null
                                    replyingToName = null
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = "Send reply", tint = AppAccent)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            item {
                PostItem(post, onMediaClick = onMediaClick)
                TextButton(
                    onClick = {
                        replyingToId = null
                        replyingToName = post.authorName
                    },
                    modifier = Modifier.padding(start = 72.dp, bottom = 4.dp)
                ) {
                    Text("Reply to @${post.authorHandle}", color = AppAccent)
                }
                HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
            }

            items(localReplies) { reply ->
                ReplyItem(
                    reply = reply,
                    collapsedReplyIds = collapsedReplyIds,
                    repostedReplyIds = repostedReplyIds,
                    bookmarkedReplyIds = bookmarkedReplyIds,
                    onReplyClicked = { id, authorName ->
                        replyingToId = id
                        replyingToName = authorName
                    },
                    onLikeClicked = { id ->
                        onReplyLike(id)
                    },
                    onRepostClicked = { id ->
                        repostedReplyIds[id] = !(repostedReplyIds[id] ?: false)
                    },
                    onBookmarkClicked = { id ->
                        bookmarkedReplyIds[id] = !(bookmarkedReplyIds[id] ?: false)
                    }
                )
                HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ReplyItem(
    reply: PostReply,
    depth: Int = 0,
    collapsedReplyIds: MutableMap<Int, Boolean>,
    repostedReplyIds: MutableMap<Int, Boolean>,
    bookmarkedReplyIds: MutableMap<Int, Boolean>,
    onReplyClicked: (id: Int, authorName: String) -> Unit,
    onLikeClicked: (id: Int) -> Unit,
    onRepostClicked: (id: Int) -> Unit,
    onBookmarkClicked: (id: Int) -> Unit
) {
    val isCollapsed = collapsedReplyIds[reply.id] ?: false
    val indentLevel = depth.coerceAtMost(4)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (16 + (indentLevel * 20)).dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(reply.authorAvatarColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reply.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${normalizedHandle(reply.authorHandle)} · ${formatTwitterTime(reply.timestamp)}", color = AppTextSecondary, fontSize = 12.sp)
            }
            Text(reply.content, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onReplyClicked(reply.id, reply.authorHandle) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        null,
                        tint = AppTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLikeClicked(reply.id) }) {
                    Icon(
                        if (reply.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        null,
                        tint = if (reply.isLiked) Color(0xFFE53935) else AppTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    if (reply.likes.isNotBlank() && reply.likes != "0") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(reply.likes, color = AppTextSecondary, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                IconButton(
                    onClick = { onRepostClicked(reply.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.Repeat,
                        null,
                        tint = if (repostedReplyIds[reply.id] == true) AppAccent else AppTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))

                IconButton(
                    onClick = { onBookmarkClicked(reply.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Outlined.BookmarkBorder,
                        null,
                        tint = if (bookmarkedReplyIds[reply.id] == true) AppAccent else AppTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Nested Replies
            if (reply.nestedReplies.isNotEmpty()) {
                Text(
                    text = if (isCollapsed) "Show ${reply.nestedReplies.size} replies" else "Hide replies",
                    color = AppAccent,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { collapsedReplyIds[reply.id] = !isCollapsed }
                )
                if (!isCollapsed) {
                    reply.nestedReplies.forEach { nested ->
                        ReplyItem(
                            reply = nested,
                            depth = depth + 1,
                            collapsedReplyIds = collapsedReplyIds,
                            repostedReplyIds = repostedReplyIds,
                            bookmarkedReplyIds = bookmarkedReplyIds,
                            onReplyClicked = onReplyClicked,
                            onLikeClicked = onLikeClicked,
                            onRepostClicked = onRepostClicked,
                            onBookmarkClicked = onBookmarkClicked,
                        )
                    }
                }
            }
        }
    }
}

private fun appendReplyToParent(replies: List<PostReply>, parentId: Int, newReply: PostReply): List<PostReply> {
    return replies.map { reply ->
        if (reply.id == parentId) {
            reply.copy(nestedReplies = reply.nestedReplies + newReply)
        } else {
            reply.copy(nestedReplies = appendReplyToParent(reply.nestedReplies, parentId, newReply))
        }
    }
}

private fun normalizedHandle(handle: String): String {
    if (handle.isBlank()) return "@user"
    return if (handle.startsWith("@")) handle else "@$handle"
}

@Composable
private fun MediaAttachmentGrid(mediaUrls: List<String>, onMediaClick: (index: Int) -> Unit = {}) {
    val items = mediaUrls

    when (items.size) {
        1 -> {
            MediaGridItem(
                mediaUrl = items[0],
                onClick = { onMediaClick(0) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }

        2 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                MediaGridItem(items[0], Modifier.weight(1f).height(200.dp), onClick = { onMediaClick(0) })
                MediaGridItem(items[1], Modifier.weight(1f).height(200.dp), onClick = { onMediaClick(1) })
            }
        }

        3 -> {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                MediaGridItem(items[0], Modifier.weight(1f).height(220.dp), onClick = { onMediaClick(0) })
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    MediaGridItem(items[1], Modifier.fillMaxWidth().height(107.dp), onClick = { onMediaClick(1) })
                    MediaGridItem(items[2], Modifier.fillMaxWidth().height(107.dp), onClick = { onMediaClick(2) })
                }
            }
        }

        4 -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    MediaGridItem(items[0], Modifier.weight(1f).height(130.dp), onClick = { onMediaClick(0) })
                    MediaGridItem(items[1], Modifier.weight(1f).height(130.dp), onClick = { onMediaClick(1) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    MediaGridItem(items[2], Modifier.weight(1f).height(130.dp), onClick = { onMediaClick(2) })
                    MediaGridItem(items[3], Modifier.weight(1f).height(130.dp), onClick = { onMediaClick(3) })
                }
            }
        }

        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                items.chunked(2).forEachIndexed { rowIndex, rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        rowItems.forEachIndexed { indexInRow, media ->
                            val absoluteIndex = (rowIndex * 2) + indexInRow
                            MediaGridItem(media, Modifier.weight(1f).height(130.dp), onClick = { onMediaClick(absoluteIndex) })
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaGridItem(mediaUrl: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2A30))
    ) {
        AsyncImage(
            model = mediaUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isVideoUrl(mediaUrl)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaViewerOverlay(
    mediaUrls: List<String>,
    initialIndex: Int,
    onClose: () -> Unit
) {
    val safeInitial = initialIndex.coerceIn(0, (mediaUrls.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitial, pageCount = { mediaUrls.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { pageIndex ->
            val mediaUrl = mediaUrls[pageIndex]
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = mediaUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                if (isVideoUrl(mediaUrl)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close media", tint = Color.White)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${pagerState.currentPage + 1}/${mediaUrls.size}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}

private fun isVideoUrl(url: String): Boolean {
    val u = url.lowercase()
    return u.endsWith(".mp4") || u.endsWith(".mov") || u.endsWith(".webm") || u.endsWith(".mkv")
}

private fun isVideoUri(context: Context, uri: Uri): Boolean {
    val mime = context.contentResolver.getType(uri)?.lowercase() ?: return false
    return mime.startsWith("video/")
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index)
        }
    }
    return null
}

private suspend fun uploadComposerMedia(context: Context, mediaUris: List<Uri>): List<String> {
    if (mediaUris.isEmpty()) return emptyList()

    android.util.Log.d("MEDIA_UPLOAD", "Starting upload: ${mediaUris.size} files selected")

    return withContext(Dispatchers.IO) {
        val uploaded = mutableListOf<String>()
        mediaUris.forEachIndexed { index, uri ->
            try {
                android.util.Log.d("MEDIA_UPLOAD", "Uploading file ${index + 1}/${mediaUris.size}: $uri")
                
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val fallbackExt = if (mimeType.startsWith("video/")) "mp4" else "jpg"
                val fileName = queryDisplayName(context, uri) ?: "upload_${System.currentTimeMillis()}_${index}.$fallbackExt"
                val tempFile = File(context.cacheDir, "composer_$fileName")

                android.util.Log.d("MEDIA_UPLOAD", "File name: $fileName, MIME: $mimeType")

                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    android.util.Log.w("MEDIA_UPLOAD", "Failed to open input stream for file ${index + 1}")
                    return@forEachIndexed
                }

                inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                android.util.Log.d("MEDIA_UPLOAD", "File copied to temp: ${tempFile.length()} bytes")

                val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
                
                android.util.Log.d("MEDIA_UPLOAD", "Uploading to server...")
                val response = ApiClient.apiService.uploadMedia(part)
                
                if (response.isSuccessful) {
                    val url = response.body()?.url
                    if (url != null) {
                        uploaded.add(url)
                        android.util.Log.d("MEDIA_UPLOAD", "Upload success ${uploaded.size}/${mediaUris.size}: $url")
                    } else {
                        android.util.Log.w("MEDIA_UPLOAD", "Upload success but no URL in response")
                    }
                } else {
                    android.util.Log.e("MEDIA_UPLOAD", "Upload failed. Status: ${response.code()}, Message: ${response.message()}")
                }

                tempFile.delete()
            } catch (e: Exception) {
                android.util.Log.e("MEDIA_UPLOAD", "Exception uploading file ${index + 1}: ${e.message}", e)
            }
        }
        
        android.util.Log.d("MEDIA_UPLOAD", "Upload complete: ${uploaded.size}/${mediaUris.size} files uploaded")
        uploaded
    }
}

private fun parseEngagement(value: String): Long {
    val v = value.trim().lowercase()
    return when {
        v.endsWith("k") -> (v.dropLast(1).toDoubleOrNull()?.times(1_000))?.toLong() ?: 0
        v.endsWith("m") -> (v.dropLast(1).toDoubleOrNull()?.times(1_000_000))?.toLong() ?: 0
        else -> v.filter { it.isDigit() }.toLongOrNull() ?: 0
    }
}

private fun getCurrentUserIdFromToken(): Int? {
    val token = TokenManager.getToken() ?: return null
    val parts = token.split('.')
    if (parts.size < 2) return null
    return try {
        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        val subject = JSONObject(payload).optString("sub", "")
        subject.toIntOrNull()
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ProfileView(onBack: () -> Unit, posts: List<Post>) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tshidiso", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${posts.size} Posts", color = AppTextSecondary, fontSize = 12.sp)
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                // Header Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFF673AB7), Color(0xFF9C27B0))))
                )

                // Profile Info
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .offset(y = (-40).dp)
                                .clip(CircleShape)
                                .background(Color(0xFF673AB7))
                                .padding(4.dp)
                                .background(AppBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color(0xFF673AB7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("T", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Edit profile", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("Tshidiso", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("@tshidiso_dev", color = AppTextSecondary, fontSize = 14.sp)
                    
                    Text(
                        "Fullstack Developer | Building the future with FirstApp 🚀 | Android & Cloud Enthusiast",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProfileStat("1,234", "Following")
                        ProfileStat("5,678", "Followers")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Tabs
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Posts",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    HorizontalDivider(color = AppAccent, thickness = 3.dp, modifier = Modifier.width(50.dp))
                }
            }

            items(posts) { post ->
                PostItem(post)
                HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ProfileStat(count: String, label: String) {
    Row {
        Text(count, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = AppTextSecondary, fontSize = 14.sp)
    }
}
