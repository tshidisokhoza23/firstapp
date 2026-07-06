package com.example.firstapp.media.ui.screens.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.firstapp.common.model.User
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.common.util.Resource
import com.example.firstapp.home.ui.screens.DataStateHandler
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsTabScreen(
    navController: NavController,
    shortsViewModel: ShortsViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    onDirectMessagesClick: () -> Unit = {}
) {
    val currentUser by userViewModel.currentUser.collectAsState()
    val shortsState by shortsViewModel.shorts.collectAsState()
    val selectedShortId by shortsViewModel.selectedShortId.collectAsState()
    
    var showShortComments by remember { mutableStateOf(false) }
    var showShortMoreMenu by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var activeShortIdForComments by remember { mutableStateOf<String?>(null) }
    var activeShortForMoreMenu by remember { mutableStateOf<ShortMedia?>(null) }
    val shortRepliesState by shortsViewModel.shortReplies.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        DataStateHandler(
            resource = shortsState,
            onEmpty = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No videos found", color = Color.White, fontSize = 18.sp)
                }
            },
            onError = { error ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error loading shorts", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(error ?: "Unknown error occurred", color = Color.Gray, fontSize = 14.sp)
                        Button(onClick = { shortsViewModel.fetchShorts() }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Retry")
                        }
                    }
                }
            }
        ) { shorts ->
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

        TopAppBar(
            title = { Text("Shorts", color = Color.White, fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { showTopMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                }
                DropdownMenu(
                    expanded = showTopMenu,
                    onDismissRequest = { showTopMenu = false },
                    modifier = Modifier.background(AppSecondary)
                ) {
                    DropdownMenuItem(
                        text = { Text("Direct Messages", color = Color.White) },
                        leadingIcon = { Icon(Icons.Default.Mail, null, tint = Color.White) },
                        onClick = { 
                            showTopMenu = false
                            onDirectMessagesClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = Color.White) },
                        onClick = { showTopMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Help & Feedback", color = Color.White) },
                        onClick = { showTopMenu = false }
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )

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
                    if (action == "message") {
                        navController.navigate(com.example.firstapp.ui.navigation.Screen.MediaChat.createRoute(activeShortForMoreMenu!!.creatorId))
                    }
                }
            )
        }
    }
}
