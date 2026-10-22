package com.tpsk.tj.media.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import android.content.Intent

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
            ownProfileId = currentUser?.id.orEmpty(),
            ownProfileName = currentUser?.displayName?.ifBlank { "You" } ?: "You",
            ownProfileHandle = currentUser?.username?.ifBlank { "you" } ?: "you",
            ownProfileAvatarUrl = currentUser?.avatarUrl,
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            onCategorySelected = { mediaViewModel.selectCategory(it) },
            selectedQualityMode = uiState.qualityMode,
            onQualityModeSelected = { mediaViewModel.selectQualityMode(it) },
            onContestsClick = { navController.navigate(Screen.ContestDiscovery.route) },
            onTrendingClick = { navController.navigate(Screen.TrendingFeed.route) },
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
            onRequestCreatePost = { replyId, quoteId ->
                navController.navigate(Screen.CreatePost.createRoute(replyId, quoteId))
            },
            onLikePost = { id, l, liked -> mediaViewModel.likePost(id, l, liked) },
            onRepostPost = { post -> mediaViewModel.repostPost(post.id, post.repostCount, post.isReposted) },
            onDeletePost = { mediaViewModel.deletePost(it) },
            onBookmarkPost = { mediaViewModel.bookmarkPost(it) }, 
            onPostViewed = { mediaViewModel.viewPost(it) }, 
            onProfileViewChange = { }, 
            onGetUserPosts = onGetUserPosts,
            userViewModel = userViewModel,
            mediaViewModel = mediaViewModel,
            onVotePoll = { postId, optionId -> mediaViewModel.votePoll(postId, optionId) },
            onFollowUser = { userId, _ ->
                if (userId.isNotBlank() && userId != "null") mediaViewModel.followUser(userId)
            },
            onRefresh = {
                mediaViewModel.loadFeed("standard")
                pagedPosts.refresh()
            },
            onDirectMessagesClick = { navController.navigate(Screen.DirectMessages.route) },
            onSavedPostsClick = { navController.navigate(Screen.SavedPosts.route) },
            onNotificationsClick = { navController.navigate(Screen.SocialNotification.route) },
            onShortClick = { short ->
                navController.navigate(Screen.PostDetail.createRoute(short.id))
            },
            onMediaClick = { url, type, position ->
                if (url.isNotBlank()) {
                    navController.navigate(Screen.MediaViewer.createRoute(url, type, position))
                }
            },
            onShareClick = { post ->
                val text = post.content.ifBlank { "Check this out on FirstApp" }
                val send = Intent(Intent.ACTION_SEND).apply {
                    this.type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                navController.context.startActivity(Intent.createChooser(send, "Share"))
            },
            onProfileClick = { userId ->
                if (userId.isNotBlank() && userId != "null") {
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                }
            },
            onNavigateToCircles = { navController.navigate(Screen.Circles.route) },
            onNavigateToCreatorDashboard = { navController.navigate(Screen.CreatorDashboard.route) },
            onHashtagNavigate = { tag ->
                val clean = tag.removePrefix("#").trim()
                if (clean.isNotBlank()) {
                    navController.navigate(Screen.ContestFeed.createRoute(clean))
                }
            }
        )

        // Social Feed Floating Action Button (FAB)
        FloatingActionButton(
            onClick = { navController.navigate(Screen.CreatePost.createRoute()) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Create Post",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
