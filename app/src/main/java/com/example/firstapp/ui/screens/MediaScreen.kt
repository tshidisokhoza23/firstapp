package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.Post
import com.example.firstapp.model.PostReply
import com.example.firstapp.model.PostType
import com.example.firstapp.ui.theme.*
import com.example.firstapp.util.formatTwitterTime

@Composable
fun MediaScreen(
    posts: List<Post>,
    selectedPost: Post?,
    onPostSelect: (Post?) -> Unit,
    onReplySubmit: (postId: Int, parentReplyId: Int?, content: String) -> Unit,
    onReplyLike: (replyId: Int) -> Unit
) {
    var showProfile by remember { mutableStateOf(false) }

    if (selectedPost != null) {
        PostDetailContent(
            post = selectedPost,
            onBack = { onPostSelect(null) },
            onReplySubmit = { parentReplyId, content ->
                onReplySubmit(selectedPost.id, parentReplyId, content)
            },
            onReplyLike = onReplyLike
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(posts) { post ->
                    PostItem(post, onClick = { onPostSelect(post) })
                    HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                }
            }
            
            FloatingActionButton(
                onClick = {},
                containerColor = AppAccent,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post")
            }
        }
    }
}

@Composable
fun PostItem(post: Post, onClick: () -> Unit = {}) {
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

            // Media Preview
            if (post.type != PostType.TEXT_ONLY && post.mediaPlaceholderColor != null) {
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
                PostAction(Icons.Outlined.ChatBubbleOutline, post.repliesCount)
                PostAction(Icons.Outlined.Repeat, post.shares)
                PostAction(Icons.Outlined.FavoriteBorder, post.likes)
                PostAction(Icons.Outlined.BarChart, post.views)
                Icon(Icons.Outlined.Share, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun PostAction(icon: ImageVector, count: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(count, color = AppTextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun PostDetailContent(
    post: Post,
    onBack: () -> Unit,
    onReplySubmit: (parentReplyId: Int?, content: String) -> Unit,
    onReplyLike: (replyId: Int) -> Unit
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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Post", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                PostItem(post)
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
    val isCollapsed = collapsedReplyIds[reply.id] ?: (depth >= 2)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (16 + (depth * 20)).dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
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
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    null,
                    tint = AppTextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onReplyClicked(reply.id, reply.authorHandle) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    null,
                    tint = AppTextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onLikeClicked(reply.id) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(reply.likes, color = AppTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Reply",
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onReplyClicked(reply.id, reply.authorHandle) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    Icons.Outlined.Repeat,
                    null,
                    tint = if (repostedReplyIds[reply.id] == true) AppAccent else AppTextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onRepostClicked(reply.id) }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    Icons.Outlined.BookmarkBorder,
                    null,
                    tint = if (bookmarkedReplyIds[reply.id] == true) AppAccent else AppTextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onBookmarkClicked(reply.id) }
                )
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
