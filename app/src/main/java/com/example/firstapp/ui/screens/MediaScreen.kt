package com.example.firstapp.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.model.Post
import com.example.firstapp.model.PostReply
import com.example.firstapp.model.PostType
import com.example.firstapp.model.Song
import com.example.firstapp.ui.theme.*
import com.example.firstapp.util.formatTwitterTime
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
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
    songs: List<Song> = emptyList(),
    mediaFeedTab: Int,
    selectedPost: Post?,
    openOwnProfileRequest: Int = 0,
    closeOwnProfileRequest: Int = 0,
    ownProfileName: String = "You",
    ownProfileHandle: String = "you",
    ownProfileAvatarColor: Color = Color(0xFF3A4B57),
    onPostSelect: (Post?) -> Unit,
    onCreatePost: (content: String, mediaUrls: List<String>) -> Unit,
    onLikePost: (postId: Int) -> Unit,
    onRepostPost: (postId: Int) -> Unit,
    onReplySubmit: (postId: Int, parentReplyId: Int?, content: String) -> Unit,
    onReplyLike: (replyId: Int) -> Unit,
    onReplyRepost: (replyId: Int) -> Unit,
    onBookmarkPost: (postId: Int) -> Unit,
    onBookmarkReply: (replyId: Int) -> Unit,
    onViewPost: (postId: Int) -> Unit = {},
    onProfileViewChange: (Boolean) -> Unit = {}
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
    var localSongsFallback by remember { mutableStateOf<List<Song>>(emptyList()) }
    val optimisticPostLikes = remember { mutableStateMapOf<Int, Pair<Boolean, String>>() }
    val optimisticPostReposts = remember { mutableStateMapOf<Int, Pair<Boolean, String>>() }
    val optimisticPostBookmarks = remember { mutableStateMapOf<Int, Boolean>() }
    var selectedProfile by remember { mutableStateOf<ProfileUser?>(null) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        selectedMedia = (selectedMedia + uris).distinct().take(10)
    }

    val currentUserId = remember { getCurrentUserIdFromToken() }
    val effectiveSongs = if (songs.isNotEmpty()) songs else localSongsFallback

    LaunchedEffect(songs.size) {
        if (songs.isEmpty() && localSongsFallback.isEmpty()) {
            runCatching {
                val response = ApiClient.apiService.getSongs()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    body.songs.map { apiSong ->
                        Song(
                            id = apiSong.id,
                            title = apiSong.title,
                            artist = apiSong.artist,
                            artistId = apiSong.artist_id ?: "",
                            albumArtUrl = apiSong.album_art_url,
                            albumArtColor = Color(0xFF6A1B9A),
                            audioUrl = apiSong.audio_url ?: "",
                            duration = apiSong.duration ?: "",
                            category = apiSong.category ?: "",
                            isLiked = apiSong.is_liked,
                            isExplicit = apiSong.is_explicit
                        )
                    }
                } else {
                    emptyList()
                }
            }.onSuccess { fallbackSongs ->
                if (fallbackSongs.isNotEmpty()) {
                    localSongsFallback = fallbackSongs
                }
            }
        }
    }

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

    val displayedPostsWithOptimistic = remember(
        displayedPosts,
        optimisticPostLikes.toMap(),
        optimisticPostReposts.toMap(),
        optimisticPostBookmarks.toMap()
    ) {
        displayedPosts.map { post ->
            var updated = post
            optimisticPostLikes[post.id]?.let { (liked, likes) ->
                updated = updated.copy(isLiked = liked, likes = likes)
            }
            optimisticPostReposts[post.id]?.let { (reposted, shares) ->
                updated = updated.copy(isReposted = reposted, shares = shares)
            }
            optimisticPostBookmarks[post.id]?.let { bookmarked ->
                updated = updated.copy(isBookmarked = bookmarked)
            }
            updated
        }
    }

    val selectedPostWithOptimistic = selectedPost?.let { base ->
        var updated = base
        optimisticPostLikes[base.id]?.let { (liked, likes) ->
            updated = updated.copy(isLiked = liked, likes = likes)
        }
        optimisticPostReposts[base.id]?.let { (reposted, shares) ->
            updated = updated.copy(isReposted = reposted, shares = shares)
        }
        optimisticPostBookmarks[base.id]?.let { bookmarked ->
            updated = updated.copy(isBookmarked = bookmarked)
        }
        updated
    }

    val allPostsWithOptimistic = remember(
        posts,
        optimisticPostLikes.toMap(),
        optimisticPostReposts.toMap(),
        optimisticPostBookmarks.toMap()
    ) {
        posts.map { post ->
            var updated = post
            optimisticPostLikes[post.id]?.let { (liked, likes) ->
                updated = updated.copy(isLiked = liked, likes = likes)
            }
            optimisticPostReposts[post.id]?.let { (reposted, shares) ->
                updated = updated.copy(isReposted = reposted, shares = shares)
            }
            optimisticPostBookmarks[post.id]?.let { bookmarked ->
                updated = updated.copy(isBookmarked = bookmarked)
            }
            updated
        }
    }

    val openProfile: (String, String, Color) -> Unit = { authorName, authorHandle, avatarColor ->
        selectedProfile = ProfileUser(
            name = authorName,
            handle = normalizedHandle(authorHandle),
            avatarColor = avatarColor
        )
    }

    LaunchedEffect(openOwnProfileRequest) {
        if (openOwnProfileRequest > 0) {
            selectedProfile = ProfileUser(
                name = ownProfileName,
                handle = normalizedHandle(ownProfileHandle),
                avatarColor = ownProfileAvatarColor
            )
        }
    }

    LaunchedEffect(closeOwnProfileRequest) {
        if (closeOwnProfileRequest > 0) {
            selectedProfile = null
        }
    }

    LaunchedEffect(selectedProfile != null) {
        onProfileViewChange(selectedProfile != null)
    }

    if (selectedProfile != null) {
        ArtistProfileContent(
            user = selectedProfile!!,
            posts = allPostsWithOptimistic,
            songs = effectiveSongs,
            onBack = { selectedProfile = null },
            onPostSelect = onPostSelect,
            onAuthorClick = openProfile
        )
    } else if (selectedPostWithOptimistic != null) {
        PostDetailContent(
            post = selectedPostWithOptimistic,
            songs = effectiveSongs,
            onBack = { onPostSelect(null) },
            onLikePost = {
                val nextLiked = !selectedPostWithOptimistic.isLiked
                val nextLikes = adjustCountString(selectedPostWithOptimistic.likes, nextLiked)
                optimisticPostLikes[selectedPostWithOptimistic.id] = nextLiked to nextLikes
                onLikePost(selectedPostWithOptimistic.id)
            },
            onRepostPost = {
                val nextReposted = !selectedPostWithOptimistic.isReposted
                val nextShares = adjustCountString(selectedPostWithOptimistic.shares, nextReposted)
                optimisticPostReposts[selectedPostWithOptimistic.id] = nextReposted to nextShares
                onRepostPost(selectedPostWithOptimistic.id)
            },
            onReplySubmit = { parentReplyId, content ->
                onReplySubmit(selectedPostWithOptimistic.id, parentReplyId, content)
            },
            onReplyLike = onReplyLike,
            onReplyRepost = onReplyRepost,
            onBookmarkPost = {
                val nextBookmarked = !selectedPostWithOptimistic.isBookmarked
                optimisticPostBookmarks[selectedPostWithOptimistic.id] = nextBookmarked
                onBookmarkPost(selectedPostWithOptimistic.id)
            },
            onBookmarkReply = onBookmarkReply,
            onMediaClick = { urls, startIndex ->
                viewerMediaUrls = urls
                viewerInitialIndex = startIndex
            },
            onAuthorClick = openProfile
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            if (displayedPosts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No posts yet in this feed", color = AppTextSecondary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(displayedPostsWithOptimistic) { post ->
                        LaunchedEffect(post.id) { onViewPost(post.id) }
                        PostItem(
                            post,
                            songs = effectiveSongs,
                            onClick = { onPostSelect(post) },
                            onLikeClick = {
                                val nextLiked = !post.isLiked
                                val nextLikes = adjustCountString(post.likes, nextLiked)
                                optimisticPostLikes[post.id] = nextLiked to nextLikes
                                onLikePost(post.id)
                            },
                            onRepostClick = {
                                val nextReposted = !post.isReposted
                                val nextShares = adjustCountString(post.shares, nextReposted)
                                optimisticPostReposts[post.id] = nextReposted to nextShares
                                onRepostPost(post.id)
                            },
                            onBookmarkClick = {
                                val nextBookmarked = !post.isBookmarked
                                optimisticPostBookmarks[post.id] = nextBookmarked
                                onBookmarkPost(post.id)
                            },
                            onMediaClick = { urls, startIndex ->
                                viewerMediaUrls = urls
                                viewerInitialIndex = startIndex
                            },
                            onAvatarClick = {
                                openProfile(post.authorName, post.authorHandle, post.authorAvatarColor)
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
    songs: List<Song> = emptyList(),
    onClick: () -> Unit = {},
    onLikeClick: () -> Unit = {},
    onRepostClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
    onMediaClick: (mediaUrls: List<String>, startIndex: Int) -> Unit = { _, _ -> },
    onAvatarClick: () -> Unit = {}
) {
    val mediaBadgeLabel = musicBadgeLabel(post.tags)
    val postMusicIcon = postMusicTypeIcon(post.tags)

    val isAlbumPost = hasTag(post.tags, "album")
    val isPlaylistPost = hasTag(post.tags, "playlist")
    var showTracks by remember { mutableStateOf(false) }
    val likeIcon = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder

    val tracksForCard = resolveMusicPostTracks(post, songs)

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
                    .clickable { onAvatarClick() }
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

            if (post.mediaUrls.isEmpty()) {
                InlineSponsoredAd(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp)
                )
            }

            // Media Preview (supports multiple images/videos)
            if (post.mediaUrls.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    MediaAttachmentGrid(
                        mediaUrls = post.mediaUrls,
                        onMediaClick = { index -> onMediaClick(post.mediaUrls, index) }
                    )

                    if (mediaBadgeLabel != null) {
                        MusicPostBadge(
                            label = mediaBadgeLabel,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    }
                }
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

                    if (mediaBadgeLabel != null) {
                        MusicPostBadge(
                            label = mediaBadgeLabel,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        )
                    }
                }
            }

            // Track panel for album/playlist posts
            if ((isAlbumPost || isPlaylistPost) && tracksForCard.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF121A20))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTracks = !showTracks },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = AppAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAlbumPost) "Album • ${tracksForCard.size} tracks" else "Playlist • ${tracksForCard.size} tracks",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (showTracks) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = AppTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (showTracks) {
                        Spacer(modifier = Modifier.height(6.dp))
                        tracksForCard.forEachIndexed { index, song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${index + 1}", color = AppTextSecondary, fontSize = 11.sp, modifier = Modifier.width(18.dp))
                                Text(song.title, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text(song.duration, color = AppTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PostActionIconOnly(Icons.Outlined.ChatBubbleOutline, count = post.repliesCount, onClick = onClick)
            PostActionIconOnly(
                Icons.Outlined.Repeat,
                count = post.shares,
                tint = if (post.isReposted) AppAccent else AppTextSecondary,
                onClick = onRepostClick
            )
            PostActionIconOnly(likeIcon, count = post.likes, onClick = onLikeClick)
            PostActionIconOnly(Icons.Outlined.BarChart, count = post.views)
            if (postMusicIcon != null) {
                Icon(
                    postMusicIcon,
                    contentDescription = "Music content",
                    tint = AppAccent,
                    modifier = Modifier.size(24.dp).clickable { onClick() }
                )
            }
            Icon(
                if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                tint = if (post.isBookmarked) AppAccent else AppTextSecondary,
                modifier = Modifier.size(24.dp).clickable { onBookmarkClick() }
            )
            Icon(Icons.Outlined.Share, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun MusicPostBadge(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InlineSponsoredAd(modifier: Modifier = Modifier, compact: Boolean = false) {
    val context = LocalContext.current
    var nativeAd by remember(compact) { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(compact) {
        val adLoader = AdLoader.Builder(context, "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { loadedAd ->
                nativeAd?.destroy()
                nativeAd = loadedAd
            }
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    LaunchedEffect(Unit) {
        MobileAds.initialize(context)
    }

    Surface(
        modifier = modifier,
        color = Color(0xFF121A20),
        shape = RoundedCornerShape(10.dp)
    ) {
        AndroidView(
            factory = { ctx -> createInlineNativeAdView(ctx, compact) },
            update = { view ->
                nativeAd?.let { ad -> bindInlineNativeAd(view, ad) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compact) 10.dp else 12.dp,
                    vertical = if (compact) 8.dp else 10.dp
                )
        )
    }
}

private fun createInlineNativeAdView(context: Context, compact: Boolean): NativeAdView {
    val root = NativeAdView(context)
    root.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val adLabel = TextView(context).apply {
        tag = "ad_label"
        text = "Google Ad"
        textSize = if (compact) 10f else 11f
        setTextColor(android.graphics.Color.parseColor("#66E0A3"))
    }

    val mediaView = MediaView(context).apply {
        tag = "ad_media"
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            if (compact) 180 else 220
        )
    }

    val headline = TextView(context).apply {
        tag = "ad_headline"
        textSize = if (compact) 12f else 13f
        setTextColor(android.graphics.Color.WHITE)
    }

    val body = TextView(context).apply {
        tag = "ad_body"
        textSize = if (compact) 11f else 12f
        setTextColor(android.graphics.Color.parseColor("#9AA5AF"))
    }

    val cta = Button(context).apply {
        tag = "ad_cta"
        textSize = if (compact) 11f else 12f
    }

    container.addView(adLabel)
    container.addView(mediaView)
    container.addView(headline)
    container.addView(body)
    container.addView(cta)
    root.addView(container)

    root.mediaView = mediaView
    root.headlineView = headline
    root.bodyView = body
    root.callToActionView = cta

    return root
}

private fun bindInlineNativeAd(adView: NativeAdView, ad: NativeAd) {
    val headline = adView.findViewWithTag<TextView>("ad_headline")
    val body = adView.findViewWithTag<TextView>("ad_body")
    val cta = adView.findViewWithTag<Button>("ad_cta")

    headline.text = ad.headline
    body.text = ad.body ?: ""
    body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
    cta.text = ad.callToAction ?: "Learn more"
    cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE

    adView.setNativeAd(ad)
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
fun PostActionIconOnly(icon: ImageVector, count: String? = null, tint: Color = AppTextSecondary, onClick: () -> Unit = {}) {
    val finalTint = if (icon == Icons.Filled.Favorite) Color(0xFFE53935) else tint
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = finalTint,
            modifier = Modifier.size(24.dp)
        )
        if (count != null) {
            Text(count, color = AppTextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
fun PostDetailContent(
    post: Post,
    songs: List<Song> = emptyList(),
    onBack: () -> Unit,
    onLikePost: () -> Unit,
    onRepostPost: () -> Unit,
    onReplySubmit: (parentReplyId: Int?, content: String) -> Unit,
    onReplyLike: (replyId: Int) -> Unit,
    onReplyRepost: (replyId: Int) -> Unit,
    onBookmarkPost: () -> Unit,
    onBookmarkReply: (replyId: Int) -> Unit,
    onMediaClick: (mediaUrls: List<String>, startIndex: Int) -> Unit,
    onAuthorClick: (name: String, handle: String, avatarColor: Color) -> Unit = { _, _, _ -> }
) {
    var localPost by remember(post.id) { mutableStateOf(post) }
    var localReplies by remember(post.id) { mutableStateOf(post.replies) }
    var tempReplyIdSeed by remember(post.id) { mutableStateOf(-1) }
    var focusedReplyId by remember(post.id) { mutableStateOf<Int?>(null) }
    var showReplyComposer by remember(post.id) { mutableStateOf(false) }
    var showPostMenu by remember(post.id) { mutableStateOf(false) }
    var composerText by remember(post.id) { mutableStateOf("") }
    var replyTargetId by remember(post.id) { mutableStateOf<Int?>(null) }
    var replyTargetName by remember(post.id) { mutableStateOf("") }
    var replyTargetContent by remember(post.id) { mutableStateOf("") }

    LaunchedEffect(post.replies) {
        localReplies = post.replies
    }

    LaunchedEffect(post) {
        localPost = post
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
                IconButton(onClick = {
                    if (focusedReplyId != null) {
                        focusedReplyId = null
                    } else {
                        onBack()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    if (focusedReplyId == null) "Post" else "Reply",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { showPostMenu = true }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = AppTextSecondary)
                    }
                    DropdownMenu(
                        expanded = showPostMenu,
                        onDismissRequest = { showPostMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reply") },
                            onClick = {
                                showPostMenu = false
                                focusedReplyId = null
                                showReplyComposer = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Back to feed") },
                            onClick = {
                                showPostMenu = false
                                onBack()
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val focusedReply = focusedReplyId?.let { findReplyById(localReplies, it) }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (focusedReply == null) {
                item {
                    PostItem(
                        localPost,
                        songs = songs,
                        onLikeClick = {
                            val nextLiked = !localPost.isLiked
                            localPost = localPost.copy(
                                isLiked = nextLiked,
                                likes = adjustCountString(localPost.likes, nextLiked)
                            )
                            onLikePost()
                        },
                        onRepostClick = {
                            val nextReposted = !localPost.isReposted
                            localPost = localPost.copy(
                                isReposted = nextReposted,
                                shares = adjustCountString(localPost.shares, nextReposted)
                            )
                            onRepostPost()
                        },
                        onBookmarkClick = {
                            val nextBookmarked = !localPost.isBookmarked
                            localPost = localPost.copy(isBookmarked = nextBookmarked)
                            onBookmarkPost()
                        },
                        onMediaClick = onMediaClick,
                        onAvatarClick = {
                            onAuthorClick(localPost.authorName, localPost.authorHandle, localPost.authorAvatarColor)
                        }
                    )

                    HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                }

                items(localReplies) { reply ->
                    ReplyItem(
                        reply = reply,
                        onReplyClicked = { id, authorName ->
                            val target = findReplyById(localReplies, id)
                            replyTargetId = id
                            replyTargetName = authorName
                            replyTargetContent = target?.content ?: ""
                            composerText = ""
                            showReplyComposer = true
                        },
                        onLikeClicked = { id ->
                            localReplies = toggleReplyLike(localReplies, id)
                            onReplyLike(id)
                        },
                        onRepostClicked = { id ->
                            localReplies = toggleReplyRepost(localReplies, id)
                            onReplyRepost(id)
                        },
                        onBookmarkClicked = { id ->
                            localReplies = toggleReplyBookmark(localReplies, id)
                            onBookmarkReply(id)
                        },
                        onOpenThread = { id ->
                            focusedReplyId = id
                        },
                        onAuthorClick = onAuthorClick
                    )
                    HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                }
            } else {
                item {
                    ReplyPrimaryItem(
                        reply = focusedReply,
                        onReplyClick = {
                            replyTargetId = focusedReply.id
                            replyTargetName = focusedReply.authorHandle
                            replyTargetContent = focusedReply.content
                            composerText = ""
                            showReplyComposer = true
                        },
                        onLikeClick = {
                            localReplies = toggleReplyLike(localReplies, focusedReply.id)
                            onReplyLike(focusedReply.id)
                        },
                        onRepostClick = {
                            localReplies = toggleReplyRepost(localReplies, focusedReply.id)
                            onReplyRepost(focusedReply.id)
                        },
                        onBookmarkClick = {
                            localReplies = toggleReplyBookmark(localReplies, focusedReply.id)
                            onBookmarkReply(focusedReply.id)
                        },
                        onAuthorClick = onAuthorClick
                    )
                    HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                }

                items(focusedReply.nestedReplies.take(1)) { reply ->
                    ReplyItem(
                        reply = reply,
                        depth = 1,
                        onReplyClicked = { id, authorName ->
                            val target = findReplyById(localReplies, id)
                            replyTargetId = id
                            replyTargetName = authorName
                            replyTargetContent = target?.content ?: ""
                            composerText = ""
                            showReplyComposer = true
                        },
                        onLikeClicked = { id ->
                            localReplies = toggleReplyLike(localReplies, id)
                            onReplyLike(id)
                        },
                        onRepostClicked = { id ->
                            localReplies = toggleReplyRepost(localReplies, id)
                            onReplyRepost(id)
                        },
                        onBookmarkClicked = { id ->
                            localReplies = toggleReplyBookmark(localReplies, id)
                            onBookmarkReply(id)
                        },
                        onOpenThread = { id ->
                            focusedReplyId = id
                        },
                        onAuthorClick = onAuthorClick
                    )
                    HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                }
            }
        }
    }

    if (showReplyComposer) {
        AlertDialog(
            onDismissRequest = { showReplyComposer = false },
            containerColor = Color(0xFF101418),
            title = { Text("Reply", color = Color.White) },
            text = {
                Column {
                    Text("Replying to @${normalizedHandle(replyTargetName).removePrefix("@")}", color = AppAccent, fontSize = 12.sp)
                    if (replyTargetContent.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(replyTargetContent, color = AppTextSecondary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = composerText,
                        onValueChange = { composerText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("Post your reply", color = AppTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppAccent,
                            unfocusedBorderColor = AppTextSecondary.copy(alpha = 0.35f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val content = composerText.trim()
                    if (content.isBlank()) return@TextButton

                    val optimistic = PostReply(
                        id = tempReplyIdSeed,
                        authorName = "You",
                        authorHandle = "you",
                        authorAvatarColor = Color(0xFF90CAF9),
                        content = content,
                        timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date()),
                        likes = "0",
                        isLiked = false,
                        nestedReplies = emptyList()
                    )
                    tempReplyIdSeed -= 1

                    localReplies = if (replyTargetId == null) {
                        localReplies + optimistic
                    } else {
                        appendReplyToParent(localReplies, replyTargetId!!, optimistic)
                    }
                    localPost = localPost.copy(repliesCount = adjustCountString(localPost.repliesCount, true))
                    onReplySubmit(replyTargetId, content)
                    composerText = ""
                    showReplyComposer = false
                }) {
                    Text("Reply", color = AppAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplyComposer = false }) {
                    Text("Cancel", color = AppTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ReplyPrimaryItem(
    reply: PostReply,
    onReplyClick: () -> Unit,
    onLikeClick: () -> Unit,
    onRepostClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onAuthorClick: (name: String, handle: String, avatarColor: Color) -> Unit
) {
    val directReplyCount = reply.nestedReplies.size
    val likeIcon = if (reply.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(reply.authorAvatarColor)
                .clickable { onAuthorClick(reply.authorName, reply.authorHandle, reply.authorAvatarColor) }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reply.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${normalizedHandle(reply.authorHandle)} · ${formatTwitterTime(reply.timestamp)}", color = AppTextSecondary, fontSize = 14.sp)
            }

            Text(
                text = reply.content,
                color = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 6.dp),
                lineHeight = 20.sp
            )

            InlineSponsoredAd(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                compact = true
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PostActionIconOnly(Icons.Outlined.ChatBubbleOutline, count = directReplyCount.toString(), onClick = onReplyClick)
            PostActionIconOnly(
                Icons.Outlined.Repeat,
                tint = if (reply.isReposted) AppAccent else AppTextSecondary,
                onClick = onRepostClick
            )
            PostActionIconOnly(likeIcon, count = reply.likes, onClick = onLikeClick)
            PostActionIconOnly(Icons.Outlined.BarChart)
            Icon(
                if (reply.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                tint = if (reply.isBookmarked) AppAccent else AppTextSecondary,
                modifier = Modifier.size(24.dp).clickable { onBookmarkClick() }
            )
            Icon(Icons.Outlined.Share, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun ReplyItem(
    reply: PostReply,
    depth: Int = 0,
    onReplyClicked: (id: Int, authorName: String) -> Unit,
    onLikeClicked: (id: Int) -> Unit,
    onRepostClicked: (id: Int) -> Unit,
    onBookmarkClicked: (id: Int) -> Unit,
    onOpenThread: (id: Int) -> Unit,
    onAuthorClick: (name: String, handle: String, avatarColor: Color) -> Unit
) {
    val directReplyCount = reply.nestedReplies.size
    val previewChild = reply.nestedReplies.firstOrNull()
    val likeIcon = if (reply.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    var rootPosition by remember(reply.id, previewChild?.id) { mutableStateOf(Offset.Zero) }
    var connectorX by remember(reply.id, previewChild?.id) { mutableStateOf<Float?>(null) }
    var parentAvatarBottomY by remember(reply.id, previewChild?.id) { mutableStateOf<Float?>(null) }
    var childAvatarTopY by remember(reply.id, previewChild?.id) { mutableStateOf<Float?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
            .onGloballyPositioned { coordinates ->
                rootPosition = coordinates.positionInRoot()
            }
            .drawBehind {
                val x = connectorX
                val startY = parentAvatarBottomY
                val endY = childAvatarTopY
                if (previewChild != null && x != null && startY != null && endY != null && endY > startY) {
                    drawLine(
                        color = AppTextSecondary.copy(alpha = 0.35f),
                        start = Offset(x, startY),
                        end = Offset(x, endY),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(reply.authorAvatarColor)
                        .clickable { onAuthorClick(reply.authorName, reply.authorHandle, reply.authorAvatarColor) }
                        .onGloballyPositioned { coordinates ->
                            val pos = coordinates.positionInRoot()
                            connectorX = pos.x - rootPosition.x + (coordinates.size.width / 2f)
                            parentAvatarBottomY = pos.y - rootPosition.y + coordinates.size.height.toFloat()
                        }
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).clickable { onOpenThread(reply.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(reply.authorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${normalizedHandle(reply.authorHandle)} · ${formatTwitterTime(reply.timestamp)}", color = AppTextSecondary, fontSize = 12.sp)
                }
                Text(reply.content, color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))

                if (depth == 0) {
                    InlineSponsoredAd(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        compact = true
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.padding(top = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PostActionIconOnly(Icons.Outlined.ChatBubbleOutline, count = directReplyCount.toString(), onClick = { onReplyClicked(reply.id, reply.authorHandle) })
                PostActionIconOnly(
                    Icons.Outlined.Repeat,
                    tint = if (reply.isReposted) AppAccent else AppTextSecondary,
                    onClick = { onRepostClicked(reply.id) }
                )
                PostActionIconOnly(likeIcon, count = reply.likes, onClick = { onLikeClicked(reply.id) })
                PostActionIconOnly(Icons.Outlined.BarChart)
                Icon(
                    if (reply.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    tint = if (reply.isBookmarked) AppAccent else AppTextSecondary,
                    modifier = Modifier.size(24.dp).clickable { onBookmarkClicked(reply.id) }
                )
                Icon(Icons.Outlined.Share, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(24.dp))
            }
        }

        if (previewChild != null) {
            val previewChildReplyCount = previewChild.nestedReplies.size
            val previewChildLikeIcon = if (previewChild.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(previewChild.authorAvatarColor)
                        .clickable {
                            onAuthorClick(previewChild.authorName, previewChild.authorHandle, previewChild.authorAvatarColor)
                        }
                        .onGloballyPositioned { coordinates ->
                            val pos = coordinates.positionInRoot()
                            childAvatarTopY = pos.y - rootPosition.y
                        }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF121A20))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.clickable { onOpenThread(previewChild.id) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(previewChild.authorName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${normalizedHandle(previewChild.authorHandle)} · ${formatTwitterTime(previewChild.timestamp)}", color = AppTextSecondary, fontSize = 11.sp)
                            }

                            Text(
                                text = previewChild.content,
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PostActionIconOnly(Icons.Outlined.ChatBubbleOutline, count = previewChildReplyCount.toString(), onClick = { onReplyClicked(previewChild.id, previewChild.authorHandle) })
                        PostActionIconOnly(
                            Icons.Outlined.Repeat,
                            tint = if (previewChild.isReposted) AppAccent else AppTextSecondary,
                            onClick = { onRepostClicked(previewChild.id) }
                        )
                        PostActionIconOnly(previewChildLikeIcon, count = previewChild.likes, onClick = { onLikeClicked(previewChild.id) })
                        PostActionIconOnly(Icons.Outlined.BarChart)
                        Icon(
                            if (previewChild.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (previewChild.isBookmarked) AppAccent else AppTextSecondary,
                            modifier = Modifier.size(24.dp).clickable { onBookmarkClicked(previewChild.id) }
                        )
                        Icon(Icons.Outlined.Share, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Children are intentionally not auto-rendered here.
        // Users drill down by tapping a reply, then we show only one direct child in focused view.
    }
}

@Composable
private fun ArtistProfileContent(
    user: ProfileUser,
    posts: List<Post>,
    songs: List<Song>,
    onBack: () -> Unit,
    onPostSelect: (Post) -> Unit,
    onAuthorClick: (name: String, handle: String, avatarColor: Color) -> Unit
) {
    val artistPosts = remember(posts, user.handle) {
        posts.filter { handlesMatch(it.authorHandle, user.handle) }
    }
    val artistReplies = remember(posts, user.handle) {
        collectRepliesForAuthor(posts, user.handle)
    }
    val userSongs = remember(songs, user) {
        songs.filter {
            it.artist.equals(user.name, ignoreCase = true) ||
                normalizedHandle(it.artist).removePrefix("@").equals(normalizedHandle(user.handle).removePrefix("@"), ignoreCase = true)
        }
    }
    val likedPosts = remember(artistPosts) { artistPosts.filter { parseEngagement(it.likes) > 0 }.sortedByDescending { parseEngagement(it.likes) } }
    val songPosts = remember(artistPosts) { artistPosts.filter { hasTag(it.tags, "song") } }
    val albumPosts = remember(artistPosts) { artistPosts.filter { hasTag(it.tags, "album") } }
    val playlistPosts = remember(artistPosts) { artistPosts.filter { hasTag(it.tags, "playlist") } }

    val isArtist = userSongs.isNotEmpty()
    val sections = remember(isArtist) {
        buildList {
            add(ProfileSection.POSTS)
            add(ProfileSection.REPLIES)
            add(ProfileSection.LIKES)
            add(ProfileSection.PLAYLISTS)
            if (isArtist) {
                add(ProfileSection.SONGS)
                add(ProfileSection.ALBUMS)
            }
        }
    }
    var selectedSection by remember(user.handle, isArtist) { mutableStateOf(ProfileSection.POSTS) }
    LaunchedEffect(sections) {
        if (!sections.contains(selectedSection)) selectedSection = ProfileSection.POSTS
    }

    Scaffold(containerColor = AppBackground) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    user.avatarColor.copy(alpha = 0.7f),
                                    Color(0xFF162028),
                                    AppBackground
                                )
                            )
                        )
                ) {
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
                        Text(if (isArtist) "Artist Profile" else "Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 8.dp)
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(user.avatarColor)
                    )
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        if (isArtist) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.Verified, contentDescription = null, tint = AppAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(normalizedHandle(user.handle), color = AppTextSecondary, fontSize = 14.sp)
                }
            }

            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sections) { section ->
                        val selected = selectedSection == section
                        AssistChip(
                            onClick = { selectedSection = section },
                            label = {
                                Text(
                                    when (section) {
                                        ProfileSection.POSTS -> "Posts"
                                        ProfileSection.REPLIES -> "Replies"
                                        ProfileSection.LIKES -> "Likes"
                                        ProfileSection.PLAYLISTS -> "Playlists"
                                        ProfileSection.SONGS -> "Songs"
                                        ProfileSection.ALBUMS -> "Albums"
                                    }
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selected) AppAccent.copy(alpha = 0.2f) else Color(0xFF121A20),
                                labelColor = if (selected) Color.White else AppTextSecondary
                            )
                        )
                    }
                }
            }

            when (selectedSection) {
                ProfileSection.POSTS -> {
                    if (artistPosts.isEmpty()) {
                        item { Text("No posts yet", color = AppTextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    } else {
                        items(artistPosts) { artistPost ->
                            PostItem(
                                post = artistPost,
                                songs = songs,
                                onClick = { onPostSelect(artistPost) },
                                onAvatarClick = { onAuthorClick(artistPost.authorName, artistPost.authorHandle, artistPost.authorAvatarColor) }
                            )
                            HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                        }
                    }
                }

                ProfileSection.REPLIES -> {
                    if (artistReplies.isEmpty()) {
                        item { Text("No replies yet", color = AppTextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    } else {
                        items(artistReplies) { entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(entry.reply.authorAvatarColor)
                                            .clickable {
                                                onAuthorClick(entry.reply.authorName, entry.reply.authorHandle, entry.reply.authorAvatarColor)
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(entry.reply.authorName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "${normalizedHandle(entry.reply.authorHandle)} · ${formatTwitterTime(entry.reply.timestamp)}",
                                                color = AppTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            "Replying on @${normalizedHandle(entry.postAuthorHandle).removePrefix("@")}\'s post",
                                            color = AppAccent,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Text(entry.reply.content, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                                TextButton(
                                    onClick = { posts.firstOrNull { it.id == entry.postId }?.let { onPostSelect(it) } },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("View post", color = AppAccent, fontSize = 12.sp)
                                }
                            }
                            HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                        }
                    }
                }

                ProfileSection.LIKES -> {
                    if (likedPosts.isEmpty()) {
                        item { Text("No liked posts yet", color = AppTextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    } else {
                        items(likedPosts) { likedPost ->
                            PostItem(
                                post = likedPost,
                                songs = songs,
                                onClick = { onPostSelect(likedPost) },
                                onAvatarClick = { onAuthorClick(likedPost.authorName, likedPost.authorHandle, likedPost.authorAvatarColor) }
                            )
                            HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                        }
                    }
                }

                ProfileSection.PLAYLISTS -> {
                    if (playlistPosts.isEmpty()) {
                        item { Text("No playlist posts yet", color = AppTextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    } else {
                        items(playlistPosts) { playlistPost ->
                            PostItem(
                                post = playlistPost,
                                songs = songs,
                                onClick = { onPostSelect(playlistPost) },
                                onAvatarClick = { onAuthorClick(playlistPost.authorName, playlistPost.authorHandle, playlistPost.authorAvatarColor) }
                            )
                            HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                        }
                    }
                }

                ProfileSection.SONGS -> {
                    if (songPosts.isEmpty()) {
                        item { Text("No song posts yet", color = AppTextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    } else {
                        items(songPosts) { songPost ->
                            PostItem(
                                post = songPost,
                                songs = songs,
                                onClick = { onPostSelect(songPost) },
                                onAvatarClick = { onAuthorClick(songPost.authorName, songPost.authorHandle, songPost.authorAvatarColor) }
                            )
                            HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                        }
                    }
                }

                ProfileSection.ALBUMS -> {
                    if (albumPosts.isEmpty()) {
                        item { Text("No album posts yet", color = AppTextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                    } else {
                        items(albumPosts) { albumPost ->
                            PostItem(
                                post = albumPost,
                                songs = songs,
                                onClick = { onPostSelect(albumPost) },
                                onAvatarClick = { onAuthorClick(albumPost.authorName, albumPost.authorHandle, albumPost.authorAvatarColor) }
                            )
                            HorizontalDivider(color = Color(0xFF233138), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

private enum class ProfileSection {
    POSTS,
    REPLIES,
    LIKES,
    PLAYLISTS,
    SONGS,
    ALBUMS
}

private data class ProfileUser(
    val name: String,
    val handle: String,
    val avatarColor: Color
)

private data class ReplyProfileEntry(
    val reply: PostReply,
    val postId: Int,
    val postAuthorHandle: String
)

private fun collectRepliesForAuthor(posts: List<Post>, authorHandle: String): List<ReplyProfileEntry> {
    fun walk(replies: List<PostReply>, post: Post, result: MutableList<ReplyProfileEntry>) {
        replies.forEach { reply ->
            if (handlesMatch(reply.authorHandle, authorHandle)) {
                result.add(ReplyProfileEntry(reply = reply, postId = post.id, postAuthorHandle = post.authorHandle))
            }
            walk(reply.nestedReplies, post, result)
        }
    }

    val result = mutableListOf<ReplyProfileEntry>()
    posts.forEach { post -> walk(post.replies, post, result) }
    return result
}

private fun handlesMatch(left: String, right: String): Boolean {
    return normalizedHandle(left).removePrefix("@").equals(normalizedHandle(right).removePrefix("@"), ignoreCase = true)
}

private fun hasTag(tags: List<String>, expected: String): Boolean {
    return tags.any { it.equals(expected, ignoreCase = true) }
}

private fun inferAlbumNameFromPost(post: Post, songs: List<Song>): String {
    val fromTag = post.tags.firstOrNull { it.startsWith("album:", ignoreCase = true) }
        ?.substringAfter(":")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    if (fromTag != null) return fromTag

    val content = post.content.trim()
    val titleLikePrefix = Regex("^([A-Za-z0-9'&\\- ]+?)\\s+(is|carries|was|returns|marks|drops)\\b", RegexOption.IGNORE_CASE)
    val titleMatch = titleLikePrefix.find(content)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.trimEnd('.', '!', '?')
        ?.takeIf { it.isNotBlank() }
    if (titleMatch != null) {
        val matchedAlbum = songs
            .map { it.albumName }
            .filter { it.isNotBlank() }
            .distinct()
            .firstOrNull { it.equals(titleMatch, ignoreCase = true) }
        if (matchedAlbum != null) return matchedAlbum
    }

    val normalizedContent = post.content.lowercase()
    return songs
        .map { it.albumName }
        .filter { it.isNotBlank() }
        .distinct()
        .firstOrNull { normalizedContent.contains(it.lowercase()) }
        .orEmpty()
}

private fun inferPlaylistNameFromPost(post: Post): String {
    val fromTag = post.tags.firstOrNull { it.startsWith("playlist:", ignoreCase = true) }
        ?.substringAfter(":")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
    if (fromTag != null) return fromTag

    val content = post.content.trim()
    val patterns = listOf(
        Regex("new\\s+playlist\\s+live:\\s*([A-Za-z0-9'\\- ]+)", RegexOption.IGNORE_CASE),
        Regex("^([A-Za-z0-9'\\- ]+?)\\s+is\\s+", RegexOption.IGNORE_CASE)
    )
    patterns.forEach { pattern ->
        val match = pattern.find(content)
        val value = match?.groupValues?.getOrNull(1)?.trim()?.trimEnd('.', '!', '?')
        if (!value.isNullOrBlank()) return value
    }

    return "Playlist"
}

private fun inferPlaylistTracksFromPost(playlistName: String, songs: List<Song>): List<Song> {
    if (songs.isEmpty()) return emptyList()

    val normalizedName = playlistName.lowercase()
    val sortedByPlays = songs.sortedByDescending { it.playCount.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L }

    val curatedPlaylistTitles = mapOf(
        "alice essentials" to listOf(
            "After Midnight", "City Lights Intro", "Neon Rain (Title Track)",
            "Ocean Echo", "Langa", "No Sleep in Summer",
            "Harbourline", "Down Bad", "Signal from You",
            "Current", "Pulse", "Rooftop",
            "Golden Hour", "Undertow", "Midnight Tides (Closer)",
            "Still Waters", "Homecoming", "City Lights (Outro)",
            "Tidal", "Anchor"
        ),
        "cape town sessions" to listOf(
            "Ocean Echo", "Rooftop", "Table Mountain",
            "Evening Swell", "De Waal Drive", "Langa",
            "Southeaster", "Waterfront Lights", "Cape Town Nights (Outro)",
            "Signal from You", "Atlantic Breeze", "Bo-Kaap",
            "Lion's Head", "Sea Point Sunset", "Bree Street",
            "Peninsula", "Green Point", "Kalk Bay",
            "Muizenberg", "Homecoming"
        ),
        "late night writing" to listOf(
            "Harbourline", "Low Tide", "3AM",
            "Salt & Silence", "Deep", "Neon Interlude",
            "Signal (Reprise)", "Wave Break", "Midnight Tides (Closer)",
            "Glass City", "Waterfront Lights", "No Sleep in Summer",
            "Phosphorescence", "Drift", "Pull",
            "Foam", "Brine", "Still Waters",
            "Haze", "Blue Frequency"
        )
    )

    val directMatchTitles = curatedPlaylistTitles.entries
        .firstOrNull { normalizedName.contains(it.key) }
        ?.value
        .orEmpty()

    if (directMatchTitles.isNotEmpty()) {
        val byTitle = songs.associateBy { it.title.lowercase() }
        return directMatchTitles.mapNotNull { byTitle[it.lowercase()] }
    }

    return when {
        normalizedName.contains("essentials") -> sortedByPlays.take(20)
        normalizedName.contains("sessions") -> songs.filter {
            val category = it.category.lowercase()
            category.contains("house") || category.contains("amapiano") || category.contains("chill")
        }.ifEmpty { sortedByPlays }.take(20)
        normalizedName.contains("late night") || normalizedName.contains("writing") -> songs.filter {
            val category = it.category.lowercase()
            category.contains("soul") || category.contains("r&b") || category.contains("pop")
        }.ifEmpty { sortedByPlays }.take(20)
        normalizedName.contains("discover") -> songs.sortedBy { it.title.lowercase() }.take(20)
        else -> sortedByPlays.take(20)
    }
}

private fun resolveMusicPostTracks(post: Post, songs: List<Song>): List<Song> {
    if (songs.isEmpty()) return emptyList()

    return when {
        hasTag(post.tags, "album") -> {
            val albumName = post.tags.firstOrNull { it.startsWith("album:", ignoreCase = true) }
                ?.substringAfter(":")
                ?.trim()
                .orEmpty()

            val albumMatches = if (albumName.isBlank()) emptyList() else {
                songs.filter { it.albumName.equals(albumName, ignoreCase = true) }
            }

            if (albumMatches.isNotEmpty()) {
                albumMatches
            } else {
                songs.filter { it.artist.equals(post.authorName, ignoreCase = true) }
                    .sortedByDescending { it.playCount.filter { c -> c.isDigit() }.toLongOrNull() ?: 0L }
                    .take(20)
            }
        }

        hasTag(post.tags, "playlist") -> {
            val playlistName = post.tags.firstOrNull { it.startsWith("playlist:", ignoreCase = true) }
                ?.substringAfter(":")
                ?.trim()
                .orEmpty()
            inferPlaylistTracksFromPost(playlistName, songs)
        }

        else -> emptyList()
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

private fun findReplyById(replies: List<PostReply>, replyId: Int): PostReply? {
    replies.forEach { reply ->
        if (reply.id == replyId) return reply
        val nested = findReplyById(reply.nestedReplies, replyId)
        if (nested != null) return nested
    }
    return null
}

private fun adjustCountString(value: String, increment: Boolean): String {
    val current = value.filter { it.isDigit() }.toLongOrNull() ?: value.trim().toLongOrNull() ?: 0L
    val next = if (increment) current + 1 else (current - 1).coerceAtLeast(0)
    return next.toString()
}

private fun toggleReplyLike(replies: List<PostReply>, replyId: Int): List<PostReply> {
    return replies.map { reply ->
        if (reply.id == replyId) {
            val nextLiked = !reply.isLiked
            reply.copy(
                isLiked = nextLiked,
                likes = adjustCountString(reply.likes, nextLiked)
            )
        } else {
            reply.copy(nestedReplies = toggleReplyLike(reply.nestedReplies, replyId))
        }
    }
}

private fun toggleReplyRepost(replies: List<PostReply>, replyId: Int): List<PostReply> {
    return replies.map { reply ->
        if (reply.id == replyId) {
            reply.copy(isReposted = !reply.isReposted)
        } else {
            reply.copy(nestedReplies = toggleReplyRepost(reply.nestedReplies, replyId))
        }
    }
}

private fun toggleReplyBookmark(replies: List<PostReply>, replyId: Int): List<PostReply> {
    return replies.map { reply ->
        if (reply.id == replyId) {
            reply.copy(isBookmarked = !reply.isBookmarked)
        } else {
            reply.copy(nestedReplies = toggleReplyBookmark(reply.nestedReplies, replyId))
        }
    }
}

private fun normalizedHandle(handle: String): String {
    if (handle.isBlank()) return "@user"
    return if (handle.startsWith("@")) handle else "@$handle"
}

private fun musicBadgeLabel(tags: List<String>): String? {
    val normalized = tags.map { it.trim().lowercase() }.toSet()
    return when {
        "album" in normalized -> "Album"
        "playlist" in normalized -> "Playlist"
        "song" in normalized -> "Song"
        else -> null
    }
}

private fun postMusicTypeIcon(tags: List<String>): ImageVector? {
    val normalized = tags.map { it.trim().lowercase() }.toSet()
    return when {
        "album" in normalized -> Icons.Default.Album
        "playlist" in normalized -> Icons.AutoMirrored.Filled.QueueMusic
        "song" in normalized -> Icons.Default.MusicNote
        else -> null
    }
}

@Composable
private fun MediaAttachmentGrid(mediaUrls: List<String>, onMediaClick: (index: Int) -> Unit = {}) {
    val items = mediaUrls

    if (items.size == 1) {
        MediaGridItem(
            mediaUrl = items[0],
            onClick = { onMediaClick(0) },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        items.chunked(2).forEachIndexed { rowIndex, rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEachIndexed { indexInRow, media ->
                    val absoluteIndex = (rowIndex * 2) + indexInRow
                    MediaGridItem(
                        media,
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        onClick = { onMediaClick(absoluteIndex) }
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
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
    var showEditDialog by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("Tshidiso") }
    var profileHandle by remember { mutableStateOf("@tshidiso_dev") }
    var profileBio by remember {
        mutableStateOf("Fullstack Developer | Building the future with FirstApp 🚀 | Android & Cloud Enthusiast")
    }

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
                            onClick = { showEditDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Edit profile", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(profileName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(profileHandle, color = AppTextSecondary, fontSize = 14.sp)
                    
                    Text(
                        profileBio,
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

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit profile") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = profileName, onValueChange = { profileName = it }, label = { Text("Name") }, singleLine = true)
                        OutlinedTextField(value = profileHandle, onValueChange = { profileHandle = it }, label = { Text("Handle") }, singleLine = true)
                        OutlinedTextField(value = profileBio, onValueChange = { profileBio = it }, label = { Text("Bio") })
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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
