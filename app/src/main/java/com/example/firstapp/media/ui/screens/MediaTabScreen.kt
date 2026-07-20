package com.example.firstapp.media.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.ui.navigation.Screen

import androidx.paging.compose.collectAsLazyPagingItems

@Composable
fun MediaTabScreen(
    navController: NavController,
    mediaViewModel: MediaViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    val uiState by mediaViewModel.uiState.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val pagedPosts = uiState.posts.collectAsLazyPagingItems()

    LaunchedEffect(currentUser?.id) {
        if (currentUser == null) {
            userViewModel.fetchCurrentUser()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        MediaScreen(
            posts = pagedPosts,
            selectedPost = uiState.selectedPost,
            ownProfileId = currentUser?.id.orEmpty(),
            ownProfileName = currentUser?.displayName.orEmpty().ifBlank { "You" },
            ownProfileHandle = currentUser?.username.orEmpty().ifBlank { "you" },
            ownProfileAvatarUrl = currentUser?.avatarUrl,
            onPostSelect = { mediaViewModel.selectPost(it) },
            onCreatePost = { c, m, tags, t, p, l -> mediaViewModel.createPost(c, m, t, tags, pollOptions = p, location = l) },
            onLikePost = { id, l, liked -> mediaViewModel.likePost(id, l, liked) },
            onRepostPost = { mediaViewModel.repostPost(it) },
            onDeletePost = { mediaViewModel.deletePost(it) },
            onReplySubmit = { p1, p2, c -> mediaViewModel.createPostReply(p1, c, p2) }, 
            onReplyLike = { mediaViewModel.likePostReply(it) }, 
            onReplyRepost = { mediaViewModel.repostPostReply(it) }, 
            onBookmarkPost = { mediaViewModel.bookmarkPost(it) }, 
            onBookmarkReply = { mediaViewModel.bookmarkPostReply(it) }, 
            onViewPost = { mediaViewModel.viewPost(it) }, 
            onProfileViewChange = { /* Handle internal profile view if needed */ }, 
            onGetUserPosts = { userId -> mediaViewModel.getUserPosts(userId) },
            onPlaySong = { s, q -> musicViewModel.playSong(s, q) }, 
            onVotePoll = { pid, oid -> mediaViewModel.votePoll(pid, oid) },
            onFollowUser = { mediaViewModel.followUser(it) },
            onArtistClick = { artistName -> musicViewModel.requestArtist(artistName) },
            onCallClick = { userId, type -> chatViewModel.initiateCall(userId, type) },
            onCreatePostClick = 0,
            onRefresh = { },
            refreshTrigger = uiState.refreshTrigger,
            onDirectMessagesClick = { navController.navigate(Screen.MediaMessages.route) },
            onRequireLogin = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onMediaClick = { url, type ->
                navController.navigate(Screen.MediaViewer.createRoute(url, type))
            }
        )
    }
}
