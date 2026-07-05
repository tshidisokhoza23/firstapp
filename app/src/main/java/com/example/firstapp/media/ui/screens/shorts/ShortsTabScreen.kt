package com.example.firstapp.media.ui.screens.shorts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel
import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.Resource
import com.example.firstapp.home.ui.screens.DataStateHandler

import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firstapp.common.ui.viewmodel.UserViewModel

@Composable
fun ShortsTabScreen(
    navController: NavController,
    shortsViewModel: ShortsViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val shortsState by shortsViewModel.shorts.collectAsState()
    val selectedShortId by shortsViewModel.selectedShortId.collectAsState()
    
    var showShortComments by remember { mutableStateOf(false) }
    var showShortMoreMenu by remember { mutableStateOf(false) }
    var activeShortIdForComments by remember { mutableStateOf<String?>(null) }
    var activeShortForMoreMenu by remember { mutableStateOf<ShortMedia?>(null) }
    val shortRepliesState by shortsViewModel.shortReplies.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        DataStateHandler(shortsState) { shorts ->
            ShortsList(
                shorts = shorts,
                shortsViewModel = shortsViewModel,
                bottomPadding = 0.dp,
                initialShortId = selectedShortId,
                onShortDisplayed = { shortsViewModel.setSelectedShortId(null) },
                onShowComments = { id ->
                    activeShortIdForComments = id
                    shortsViewModel.fetchShortReplies(id)
                    showShortComments = true
                },
                onShowMoreMenu = { short ->
                    activeShortForMoreMenu = short
                    showShortMoreMenu = true
                },
                onCreatorClick = { creatorId ->
                    navController.navigate(com.example.firstapp.ui.navigation.Screen.ShortsProfile.createRoute(creatorId))
                }
            )
        }

        if (showShortComments && activeShortIdForComments != null) {
            ShortCommentsSheet(
                shortId = activeShortIdForComments!!,
                repliesState = shortRepliesState,
                currentUserAvatarUrl = currentUser?.avatar_url,
                onDismiss = {
                    showShortComments = false
                    activeShortIdForComments = null
                    shortsViewModel.clearActiveShortReplies()
                },
                onSendReply = { content, parentId ->
                    shortsViewModel.replyToShort(activeShortIdForComments!!, content, parentId)
                },
                onLikeReply = { replyId ->
                    shortsViewModel.likeShortReply(replyId)
                }
            )
        }

        if (showShortMoreMenu && activeShortForMoreMenu != null) {
            ShortMoreMenuSheet(
                short = activeShortForMoreMenu!!,
                onDismiss = {
                    showShortMoreMenu = false
                    activeShortForMoreMenu = null
                },
                onAction = { action ->
                    // Handle action
                }
            )
        }
    }
}
