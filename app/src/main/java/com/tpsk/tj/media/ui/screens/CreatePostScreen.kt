package com.tpsk.tj.media.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tpsk.tj.LocalCurrentUser
import com.tpsk.tj.common.data.repository.AiRepository
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.media.ui.components.AiAssistSheet
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    replyToPost: Post? = null,
    quotingPost: Post? = null,
    onDismiss: () -> Unit,
    onPostCreated: () -> Unit,
    mediaViewModel: MediaViewModel,
    aiRepository: AiRepository? = null
) {
    val currentUser = LocalCurrentUser.current

    var postText by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var showPoll by remember { mutableStateOf(false) }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var isPostingState by remember { mutableStateOf(false) }
    var showAiAssistSheet by remember { mutableStateOf(false) }
    var isAiImproved by remember { mutableStateOf(false) }
    var selectedVisibility by remember { mutableStateOf("Public") }

    val charLimit = 280
    val canPost = (postText.isNotBlank() || selectedImageUris.isNotEmpty() || showPoll) && postText.length <= charLimit

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val current = selectedImageUris.toMutableList()
            for (uri in uris) {
                if (!current.contains(uri) && current.size < 4) {
                    current.add(uri)
                }
            }
            selectedImageUris = current
        }
    }

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF8A2BE2), Color(0xFF00E5FF))
    )

    Scaffold(
        containerColor = Color(0xFF1A1A1A),
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A)),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                actions = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        val remaining = charLimit - postText.length
                        Text(
                            text = "$remaining",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remaining < 0) Color.Red else Color(0xFF00E5FF)
                        )
                        Text(
                            text = "Draft saved",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (!canPost || isPostingState) return@Button
                            isPostingState = true

                            val imageUriStrings = selectedImageUris.map { it.toString() }
                            val postType = when {
                                showPoll -> "poll"
                                replyToPost != null -> "reply"
                                quotingPost != null -> "repost"
                                else -> "standard"
                            }
                            val finalPoll = if (showPoll) pollOptions.filter { it.isNotBlank() } else null

                            mediaViewModel.createPost(
                                content = postText,
                                mediaUris = imageUriStrings,
                                postType = postType,
                                pollOptions = finalPoll,
                                location = selectedLocation,
                                parentPostId = replyToPost?.id,
                                rootPostId = replyToPost?.rootPostId ?: replyToPost?.id,
                                repostOfId = quotingPost?.id
                            )

                            onPostCreated()
                        },
                        enabled = canPost && !isPostingState,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(gradientBrush, RoundedCornerShape(20.dp))
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Share",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = Color(0xFF242424).copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, Color(0xFF333333)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Public Dropdown row
                    Row(
                        modifier = Modifier
                            .clickable { /* Select Scope */ }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$selectedVisibility ▾",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00E5FF)
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF333333))

                    // Tool Icons Row (Teal outlines)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                        ) {
                            Icon(Icons.Outlined.Image, contentDescription = "Image", tint = Color(0xFF00E5FF))
                        }

                        IconButton(
                            onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.SingleMimeType("video/*"))
                                )
                            }
                        ) {
                            Icon(Icons.Outlined.Videocam, contentDescription = "Video", tint = Color(0xFF00E5FF))
                        }

                        IconButton(
                            onClick = {
                                if (!showPoll) {
                                    showPoll = true
                                    selectedImageUris = emptyList()
                                } else {
                                    showPoll = false
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Poll, contentDescription = "Poll", tint = Color(0xFF00E5FF))
                        }

                        IconButton(onClick = { selectedLocation = "Cape Town, SA" }) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = "Location", tint = Color(0xFF00E5FF))
                        }

                        // AI Assist Sparkle Button
                        IconButton(onClick = { showAiAssistSheet = true }) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Assist",
                                tint = Color(0xFF00E5FF)
                            )
                        }

                        IconButton(onClick = { }) {
                            Icon(Icons.Outlined.Schedule, contentDescription = "Schedule", tint = Color(0xFF00E5FF))
                        }

                        IconButton(onClick = { }) {
                            Icon(Icons.Outlined.Group, contentDescription = "Collaborators", tint = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Main Input Area with Avatar Progress Ring
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(2.dp, gradientBrush, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333))
                ) {
                    if (!currentUser?.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = currentUser?.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = postText,
                        onValueChange = { postText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            color = Color.White,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(Color(0xFF00E5FF)),
                        decorationBox = { innerTextField ->
                            if (postText.isEmpty()) {
                                Text(
                                    text = if (replyToPost != null) "Post your reply..." else "Share your thoughts...",
                                    fontSize = 18.sp,
                                    color = Color.Gray
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Feeling creative pill
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { showAiAssistSheet = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Feeling creative ✨",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF00E5FF)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Template Chips (Horizontal Scroll)
            Text(
                text = "TEMPLATES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val templates = listOf(
                "🔥 Hot take" to "Hot take: ",
                "❓ Question" to "Question for the community: ",
                "🧵 Thread" to "Thread 🧵 (1/N):\n",
                "📢 Announcement" to "📢 Big announcement: "
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(templates) { (label, prefix) ->
                    Surface(
                        onClick = {
                            if (!postText.startsWith(prefix)) {
                                postText = prefix + postText
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF242424),
                        border = BorderStroke(1.dp, Color(0xFF333333))
                    ) {
                        Text(
                            text = label,
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attached Media Grid
            if (selectedImageUris.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedImageUris) { uri ->
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2A2A2A))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { selectedImageUris = selectedImageUris.filter { it != uri } },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(26.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // AI Assist Sheet Dialog
    if (showAiAssistSheet) {
        val repo = aiRepository ?: rememberAiRepository()
        AiAssistSheet(
            initialText = postText,
            aiRepository = repo,
            onDismiss = { showAiAssistSheet = false },
            onApplyText = { newText, _, isImproved ->
                postText = newText
                isAiImproved = isImproved
            }
        )
    }
}
