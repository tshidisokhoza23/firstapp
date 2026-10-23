@file:OptIn(ExperimentalMaterial3Api::class)

package com.tpsk.tj.chat.nearby.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.tpsk.tj.chat.nearby.model.NearbyPost
import com.tpsk.tj.chat.nearby.model.NearbyReply
import com.tpsk.tj.chat.nearby.ui.viewmodel.NearbyViewModel
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.common.util.formatTwitterTime
import com.tpsk.tj.ui.navigation.Screen

@Composable
fun NearbyTabScreen(
    navController: NavController,
    bottomPadding: Dp = 0.dp,
    showCityPickerTrigger: Int = 0,
    nearbyViewModel: NearbyViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by nearbyViewModel.uiState.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()

    var showRadiusPickerSheet by remember { mutableStateOf(false) }
    var showCityPickerSheet by remember { mutableStateOf(false) }
    var showNearbyDrawerSheet by remember { mutableStateOf(false) }
    var chatRequestPostTarget by remember { mutableStateOf<NearbyPost?>(null) }

    LaunchedEffect(showCityPickerTrigger) {
        if (showCityPickerTrigger > 0) {
            showCityPickerSheet = true
        }
    }

    val authorName = remember(currentUser) {
        currentUser?.displayName?.ifBlank { currentUser?.username }?.ifBlank { "You" } ?: "You"
    }
    val authorId = remember(currentUser) { currentUser?.id.orEmpty() }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { nearbyViewModel.refreshLocationAndFeed() }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (!message.isNullOrBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp + bottomPadding)
        ) {
            // Item 1: Location Warning Banner if GPS is Off
            if (!uiState.isGpsEnabled) {
                item(key = "gps_warning_banner") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocationOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Turn on device location to see live posts.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                }
                            }) {
                                Text("Turn On", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Item 2: Radar Header Section
            item(key = "radar_header") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp)
                ) {
                    // Radar Badge Icon (Centered & clean)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F2B36)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Radar",
                            tint = Color(0xFF00B4D8),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Nearby",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Discover what's happening around you",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location / Radius Chip Switcher
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { showRadiusPickerSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF00B4D8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📍 ${uiState.coarseAreaName} • Within ${uiState.radiusKm} km ▾",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Item 3: Empty Area State or Feed Posts List
            if (uiState.posts.isEmpty()) {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!uiState.hasLocationPermission) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Approximate Location Required",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Nearby needs approximate location to find posts around you.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            locationPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    Manifest.permission.ACCESS_FINE_LOCATION
                                                )
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("Allow Location")
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Explore,
                                        contentDescription = null,
                                        tint = Color(0xFF00B4D8),
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No community posts in ${uiState.coarseAreaName} yet.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Be the first to start the conversation!",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            navController.navigate(Screen.CreateNearbyPost.createRoute(uiState.coarseAreaName))
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Post to ${uiState.coarseAreaName}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(uiState.posts, key = { it.id }) { post ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        NearbyPostCard(
                            post = post,
                            currentUserAvatarUrl = currentUser?.avatarUrl,
                            currentUserId = authorId,
                            onAuthorClick = {
                                if (post.userId.isNotBlank()) {
                                    navController.navigate(Screen.UserProfile.createRoute(post.userId))
                                }
                            },
                            onLike = { nearbyViewModel.toggleLike(post.id) },
                            onReply = {
                                navController.navigate(
                                    Screen.DiscussionThread.createRoute(
                                        postId = post.id,
                                        postTitle = "${post.authorName}'s Public Thread",
                                        postSubtitle = "${post.coarseAreaName} • ${post.replyCount + 1} participants",
                                        authorName = post.authorName,
                                        authorDistance = "${post.distanceKm} km away",
                                        authorTime = formatTwitterTime(post.createdAt.toString()),
                                        postContent = post.content,
                                        authorId = post.userId
                                    )
                                )
                            },
                            onRequestChat = {
                                chatRequestPostTarget = post
                            },
                            onShare = {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${post.authorName} on Nearby (${post.coarseAreaName}): ${post.content}")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share post"))
                            },
                            onReport = {
                                nearbyViewModel.reportPost(post.id)
                                Toast.makeText(context, "Post reported & hidden", Toast.LENGTH_SHORT).show()
                            },
                            onBlockUser = {
                                nearbyViewModel.blockUser(post.userId)
                                Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        // Floating Action Button ("Post +")
        FloatingActionButton(
            onClick = {
                navController.navigate(Screen.CreateNearbyPost.createRoute(uiState.coarseAreaName))
            },
            containerColor = Color(0xFF1B3B36),
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp + bottomPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Post",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Post",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showNearbyDrawerSheet) {
        NearbyDrawerSheet(
            onDismiss = { showNearbyDrawerSheet = false },
            onNavigateMyPosts = {
                navController.navigate(Screen.MyNearbyPosts.route)
            },
            onNavigateChatRequests = {
                navController.navigate(Screen.ChatRequests.route)
            },
            onSelectLocation = {
                showCityPickerSheet = true
            }
        )
    }

    if (showRadiusPickerSheet) {
        AlertDialog(
            onDismissRequest = { showRadiusPickerSheet = false },
            title = { Text("Nearby range") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 25, 50).forEach { km ->
                        TextButton(
                            onClick = {
                                nearbyViewModel.setRadius(km)
                                showRadiusPickerSheet = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (uiState.radiusKm == km) "$km km (current)" else "$km km",
                                fontWeight = if (uiState.radiusKm == km) FontWeight.Bold else FontWeight.Normal,
                                color = Color(0xFF00B4D8)
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            showRadiusPickerSheet = false
                            showCityPickerSheet = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Change area")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRadiusPickerSheet = false }) { Text("Close") }
            }
        )
    }

    if (showCityPickerSheet) {
        AreaPickerSheet(
            viewModel = nearbyViewModel,
            currentArea = uiState.selectedCity ?: uiState.coarseAreaName,
            onDismiss = { showCityPickerSheet = false },
            onAreaSelected = {
                showCityPickerSheet = false
            }
        )
    }

    chatRequestPostTarget?.let { post ->
        PrivateChatRequestSheet(
            post = post,
            onDismiss = { chatRequestPostTarget = null },
            onSendRequest = { inquiry ->
                nearbyViewModel.sendChatRequest(
                    post = post,
                    inquiryMessage = inquiry,
                    senderId = authorId,
                    senderName = authorName,
                    senderAvatarUrl = currentUser?.avatarUrl,
                    onSuccess = {
                        chatRequestPostTarget = null
                        Toast.makeText(context, "Chat request sent to ${post.authorName}!", Toast.LENGTH_LONG).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }
}

@Composable
fun NearbyDrawerSheet(
    onDismiss: () -> Unit,
    onNavigateMyPosts: () -> Unit,
    onNavigateChatRequests: () -> Unit,
    onSelectLocation: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Nearby Options",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            ListItem(
                headlineContent = { Text("My Posts", fontWeight = FontWeight.SemiBold) },
                leadingContent = { Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF00B4D8)) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onNavigateMyPosts()
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { Text("Message Requests", fontWeight = FontWeight.SemiBold) },
                leadingContent = { Icon(Icons.Default.MarkEmailUnread, contentDescription = null, tint = Color(0xFF00B4D8)) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onNavigateChatRequests()
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            ListItem(
                headlineContent = { Text("Switch Location / Area", fontWeight = FontWeight.SemiBold) },
                leadingContent = { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00B4D8)) },
                modifier = Modifier.clickable {
                    onDismiss()
                    onSelectLocation()
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
fun PrivateChatRequestSheet(
    post: NearbyPost,
    onDismiss: () -> Unit,
    onSendRequest: (inquiryMessage: String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00B4D8)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!post.authorAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = post.authorAvatarUrl,
                            contentDescription = post.authorName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = post.authorName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Message ${post.authorName} Privately",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Send a private inquiry request without spamming inbox",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "REGARDING POST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00B4D8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = post.content,
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= 250) message = it },
                placeholder = { Text("Write your private inquiry message...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (message.trim().isNotBlank()) {
                        onSendRequest(message.trim())
                    }
                },
                enabled = message.trim().isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Private Request", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NearbyPostCard(
    post: NearbyPost,
    currentUserAvatarUrl: String?,
    currentUserId: String,
    onAuthorClick: () -> Unit = {},
    onLike: () -> Unit,
    onReply: () -> Unit,
    onRequestChat: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
    onBlockUser: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReply() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!post.authorAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = com.tpsk.tj.common.util.MediaUtils.formatUrl(post.authorAvatarUrl),
                        contentDescription = post.authorName,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onAuthorClick),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(post.authorAvatarColor)
                            .clickable(onClick = onAuthorClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.authorName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f).clickable(onClick = onAuthorClick)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " • ${post.distanceKm} km away",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${post.coarseAreaName} • ${formatTwitterTime(post.createdAt.toString())}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (post.userId != currentUserId) {
                            DropdownMenuItem(
                                text = { Text("Message Privately") },
                                leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF00B4D8)) },
                                onClick = {
                                    showMenu = false
                                    onRequestChat()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Report Post") },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onReport()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Block User") },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onBlockUser()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.content,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (post.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(post.imageUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Reply Icon + Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onReply() }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Reply",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.replyCount}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Request Chat option (for non-author)
                if (post.userId != currentUserId) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onRequestChat() }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Request Chat",
                            tint = Color(0xFF00B4D8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Request Chat",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00B4D8)
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }

                // Like Icon + Count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onLike() }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = if (post.isLiked) Color(0xFF00B4D8) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likeCount}",
                        fontSize = 13.sp,
                        color = if (post.isLiked) Color(0xFF00B4D8) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Share Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onShare() }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
