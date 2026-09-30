package com.tpsk.tj.media.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    replyToPost: Post? = null,
    quotingPost: Post? = null,
    onDismiss: () -> Unit,
    onPostCreated: () -> Unit,
    mediaViewModel: MediaViewModel
) {
    val currentUser = LocalCurrentUser.current

    var postText by remember { mutableStateOf("") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var showPoll by remember { mutableStateOf(false) }
    var pollOptions by remember { mutableStateOf(listOf("", "")) }
    var isPostingState by remember { mutableStateOf(false) }

    val charLimit = 280
    val canPost = (postText.isNotBlank() || selectedImageUris.isNotEmpty() || showPoll) && postText.length <= charLimit

    // Photo picker launcher (standard Android PhotoPicker, avoids broad storage permissions)
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // Pins bottom toolbar directly above keyboard
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
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

                            // One create path only. A second worker upload was publishing a duplicate post.
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

                            // 3. Instantly pop back to feed with zero UI lag
                            onPostCreated()
                        },
                        enabled = canPost && !isPostingState,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D9BF0),
                            disabledContainerColor = Color(0xFF1D9BF0).copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (replyToPost != null) "Reply" else "Post",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Reply permission row
                    Row(
                        modifier = Modifier
                            .clickable { /* Show Reply Scope Picker */ }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF1D9BF0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Everyone can reply",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D9BF0)
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Media action bar & Character counter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            enabled = selectedImageUris.size < 4
                        ) {
                            Icon(Icons.Outlined.Image, contentDescription = "Add Images", tint = Color(0xFF1D9BF0))
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
                            Icon(Icons.Outlined.Poll, contentDescription = "Add Poll", tint = Color(0xFF1D9BF0))
                        }

                        IconButton(onClick = { selectedLocation = "Cape Town, SA" }) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = "Add Location", tint = Color(0xFF1D9BF0))
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Character count indicator
                        val remaining = charLimit - postText.length
                        Text(
                            text = "$remaining",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (remaining < 0) Color.Red else Color.Gray,
                            modifier = Modifier.padding(end = 8.dp)
                        )
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
            if (replyToPost != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Replying to ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "@${replyToPost.authorHandle}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D9BF0)
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                // User Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1D9BF0))
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

                // Auto-expanding Post Input Field
                BasicTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF1D9BF0)),
                    decorationBox = { innerTextField ->
                        if (postText.isEmpty()) {
                            Text(
                                text = if (replyToPost != null) "Post your reply" else "What's happening?",
                                fontSize = 18.sp,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location Badge
            if (selectedLocation != null) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF1D9BF0).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF1D9BF0), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(selectedLocation!!, color = Color(0xFF1D9BF0), fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Close,
                        null,
                        tint = Color(0xFF1D9BF0),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { selectedLocation = null }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Poll Creator Options
            if (showPoll) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Poll Options", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        pollOptions.forEachIndexed { index, option ->
                            OutlinedTextField(
                                value = option,
                                onValueChange = { newValue ->
                                    val newList = pollOptions.toMutableList()
                                    newList[index] = newValue
                                    pollOptions = newList
                                },
                                placeholder = { Text("Choice ${index + 1}") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        if (pollOptions.size < 4) {
                            TextButton(
                                onClick = { pollOptions = pollOptions + "" }
                            ) {
                                Text("+ Add Choice", color = Color(0xFF1D9BF0))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Attached Media Grid with Remove (X) buttons
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
                                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quoted Post Card Preview
            if (quotingPost != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(quotingPost.authorAvatarColor)
                            ) {
                                AsyncImage(model = quotingPost.authorAvatarUrl, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(quotingPost.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(" @${quotingPost.authorHandle}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(quotingPost.content, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
