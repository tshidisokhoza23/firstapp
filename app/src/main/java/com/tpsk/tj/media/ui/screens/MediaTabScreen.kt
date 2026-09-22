package com.tpsk.tj.media.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.chat.ui.viewmodel.ChatViewModel
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import com.tpsk.tj.music.ui.viewmodel.MusicViewModel
import com.tpsk.tj.home.ui.viewmodel.MainViewModel
import com.tpsk.tj.ui.navigation.Screen
import kotlin.text.*

import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun MediaTabScreen(
    navController: NavController,
    adManager: com.tpsk.tj.common.data.ads.AdManager,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    mediaViewModel: MediaViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val uiState by mediaViewModel.uiState.collectAsState()
    val autoplayEnabled by mainViewModel.autoplayVideos.collectAsState()
    val autoplayGifsEnabled by mainViewModel.autoplayGifs.collectAsState()
    val currentUser = com.tpsk.tj.LocalCurrentUser.current
    val pagedPosts = uiState.posts.collectAsLazyPagingItems()

    LaunchedEffect(Unit) {
        mediaViewModel.loadFeed("standard")
        pagedPosts.refresh()
    }

    val onGetUserPosts = remember(mediaViewModel) {
        { userId: String, type: String -> mediaViewModel.getUserPosts(userId, type) }
    }

    Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)) {
        MediaScreen(
            adManager = adManager,
            posts = pagedPosts,
            selectedPost = uiState.selectedPost,
            ownProfileId = currentUser.id,
            ownProfileName = currentUser.displayName.ifBlank { "You" },
            ownProfileHandle = currentUser.username.ifBlank { "you" },
            ownProfileAvatarUrl = currentUser.avatarUrl,
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelected = { mediaViewModel.selectCategory(it) },
            onPostSelect = { post ->
                if (post != null) {
                    mediaViewModel.selectPost(post)
                    navController.navigate(Screen.PostDetail.createRoute(post.id))
                } else {
                    mediaViewModel.selectPost(null)
                }
            },
            onCreatePost = { c, m, tags, t, p, l, pId, rId, qId -> 
                mediaViewModel.createPost(c, m, postType = t ?: "standard", tags = tags, pollOptions = p, location = l, parentPostId = pId, rootPostId = rId, repostOfId = qId) 
            },
            onLikePost = { id, l, liked -> mediaViewModel.likePost(id, l, liked) },
            onRepostPost = { post -> mediaViewModel.repostPost(post.id, post.repostCount, post.isReposted) },
            onDeletePost = { mediaViewModel.deletePost(it) },
            onBookmarkPost = { mediaViewModel.bookmarkPost(it) }, 
            onPostViewed = { mediaViewModel.viewPost(it) }, 
            onProfileViewChange = { /* Handle internal profile view if needed */ }, 
            onGetUserPosts = onGetUserPosts,
            userViewModel = userViewModel,
            mediaViewModel = mediaViewModel,
            onPlaySong = { s, q -> musicViewModel.playSong(s, q) }, 
            onVotePoll = { pid, oid -> mediaViewModel.votePoll(pid, oid) },
            onFollowUser = { userId, isFollowed -> 
                if (isFollowed) mediaViewModel.unfollowUser(userId)
                else mediaViewModel.followUser(userId)
            },
            onArtistClick = { artistName -> musicViewModel.requestArtist(artistName) },
            onCallClick = { userId, type -> chatViewModel.startVoiceOrVideoCall(receiverId = userId, callType = type) },
            onCreatePostClick = 0,
            onRefresh = { mediaViewModel.refreshAll() },
            refreshTrigger = uiState.refreshTrigger,
            onDirectMessagesClick = { navController.navigate(Screen.MediaMessages.route) },
            onSavedPostsClick = { navController.navigate(Screen.SavedPosts.route) },
            onAdRevenueLogged = { pid, rev, uid -> mediaViewModel.onAdRevenueLogged(pid, rev, uid) },
            onShortClick = { short ->
                // REQUIREMENT: Universal Entry Point for shorts
                navController.navigate(Screen.Main.createRoute(tab = "scroll", shortId = short.id))
            },
            onMediaClick = { url, type, pos ->
                navController.navigate(Screen.MediaViewer.createRoute(url, type, pos))
            },
            onShareClick = { post ->
                mediaViewModel.sharePost(post.id, post.shares)
                com.tpsk.tj.common.util.ShareUtils.sharePost(navController.context, post)
            },
            onProfileClick = { userId ->
                navController.navigate(Screen.UserProfile.createRoute(userId))
            },
            autoplayEnabled = autoplayEnabled,
            autoplayGifsEnabled = autoplayGifsEnabled,
            bottomPadding = bottomPadding
        )
    }
}
