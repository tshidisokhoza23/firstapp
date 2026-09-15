package com.example.firstapp.mingle.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import coil.compose.AsyncImage
import com.example.firstapp.common.util.MediaUtils
import com.example.firstapp.mingle.ui.viewmodel.MingleViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MingleProfilePreviewDialog(
    item: MingleActivityNotification,
    onDismiss: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    userViewModel: UserViewModel = hiltViewModel(),
    mingleViewModel: MingleViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by mingleViewModel.uiState.collectAsState()
    val currentUser by userViewModel.currentUser.collectAsState()
    val hasUploadedProfile = remember(uiState.myPhotos, currentUser) {
        uiState.myPhotos.any { !it.url.isNullOrBlank() } || !currentUser?.avatarUrl.isNullOrBlank()
    }

    var isMatched by remember(item.id) { mutableStateOf(item.type == MingleActivityType.MATCH) }
    var showMatchOverlay by remember(item.id) { mutableStateOf(false) }
    var matchConversationId by remember(item.id) { mutableStateOf<String?>(null) }

    // Only show media actually returned by the activity response.
    val photos = remember(item) {
        listOfNotNull(item.actorAvatarUrl, item.photoUrl)
            .filter { it.isNotBlank() }
            .distinct()
            .take(5)
    }

    val horizontalPagerState = rememberPagerState(pageCount = { photos.size.coerceAtLeast(1) })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(28.dp)),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Photo Carousel
                    HorizontalPager(
                        state = horizontalPagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { photoIndex ->
                        val photoUrl = photos.getOrNull(photoIndex)
                        val formatted = MediaUtils.formatUrl(photoUrl)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(photoIndex) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            if (offset.x < size.width / 2) {
                                                if (photoIndex > 0) {
                                                    scope.launch { horizontalPagerState.animateScrollToPage(photoIndex - 1) }
                                                }
                                            } else {
                                                if (photoIndex < photos.size - 1) {
                                                    scope.launch { horizontalPagerState.animateScrollToPage(photoIndex + 1) }
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            if (formatted != null) {
                                AsyncImage(
                                    model = formatted,
                                    contentDescription = item.actorName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFFE91E63), Color(0xFF880E4F))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.actorName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 64.sp
                                    )
                                }
                            }
                        }
                    }

                    // Top Photo Indicators & Banner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                )
                            )
                            .padding(16.dp)
                    ) {
                        // Dot indicators
                        if (photos.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                photos.forEachIndexed { idx, _ ->
                                    val isSelected = idx == horizontalPagerState.currentPage
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .size(if (isSelected) 10.dp else 7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color.White
                                                else Color.White.copy(alpha = 0.4f)
                                            )
                                    )
                                }
                            }
                        }

                        // Notification Pill / Badge Banner
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isMatched) Color(0xFF2196F3).copy(alpha = 0.95f) else Color(0xFFE91E63).copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isMatched) Icons.Default.Favorite else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isMatched) "Matched! You both like each other! 🎉" else item.actionText,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Overlay Gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.45f)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                                )
                            )
                    )

                    // Unified Bottom Overlay (Profile Details + Action Controls)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        // Profile Details
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.actorName,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (photos.size > 1) "Tap left/right on the photo to view more pictures." else "Mingle activity",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Bottom Action Controls Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(32.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isMatched) {
                                // Pass Button
                                FloatingActionButton(
                                    onClick = onDismiss,
                                    containerColor = Color.White.copy(alpha = 0.25f),
                                    contentColor = Color.White,
                                    shape = CircleShape,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Pass", modifier = Modifier.size(26.dp))
                                }

                                // Like Back Button (Triggers match celebration)
                                Button(
                                    onClick = {
                                        if (!hasUploadedProfile) {
                                            Toast.makeText(context, "You must upload or complete your Mingle profile before you can like people!", Toast.LENGTH_LONG).show()
                                            return@Button
                                        }
                                        mingleViewModel.swipe(
                                            profile = MingleProfile(
                                                id = item.actorId,
                                                name = item.actorName,
                                                age = 0,
                                                distanceKm = 0,
                                                bio = "",
                                                photos = photos
                                            ),
                                            action = "like",
                                            onMatched = { conversationId ->
                                                isMatched = true
                                                showMatchOverlay = true
                                                Toast.makeText(context, "It's a Match with ${item.actorName}! 🎉", Toast.LENGTH_SHORT).show()
                                                matchConversationId = conversationId
                                            },
                                            onInsufficientPoints = {
                                                Toast.makeText(context, "Liking a profile costs 10 Wallet Points!", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Like Back ❤️", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                }
                            } else {
                                // Send Message Button when Matched
                                Button(
                                    onClick = {
                                        mingleViewModel.openChat(
                                            userId = item.actorId,
                                            candidateName = item.actorName,
                                            onOpened = { conversationId ->
                                                onDismiss()
                                                onNavigateToChat(conversationId)
                                            },
                                            onError = { message ->
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier
                                        .height(52.dp)
                                        .fillMaxWidth(0.85f)
                                ) {
                                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send Message 💬", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Match Celebration Overlay (if matched via Like Back)
                    if (showMatchOverlay && isMatched) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.92f))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(72.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "IT'S A MATCH!",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 28.sp,
                                    letterSpacing = 1.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "You and ${item.actorName} liked each other!",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Button(
                                    onClick = {
                                        val conversationId = matchConversationId
                                        if (!conversationId.isNullOrBlank()) {
                                            onDismiss()
                                            onNavigateToChat(conversationId)
                                        } else {
                                            mingleViewModel.openChat(
                                                userId = item.actorId,
                                                candidateName = item.actorName,
                                                onOpened = { openedId ->
                                                    onDismiss()
                                                    onNavigateToChat(openedId)
                                                },
                                                onError = { message ->
                                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(Icons.Default.ChatBubble, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send Message", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                TextButton(
                                    onClick = { showMatchOverlay = false }
                                ) {
                                    Text("Keep Previewing", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
