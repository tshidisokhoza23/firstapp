@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.tpsk.tj.media.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.media.model.Post
import java.util.ArrayList
import kotlin.collections.*
import kotlin.text.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostComposerDialog(
    replyingTo: Post? = null,
    quotingPost: Post? = null,
    avatarUrl: String? = null,
    avatarColor: Color = Color.Gray,
    onDismiss: () -> Unit,
    onSubmit: (String, List<String>, List<String>, String?, List<String>?, String?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedAudioUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedLocation by remember { mutableStateOf<String?>(null) }
    var showPoll by remember { mutableStateOf(false) }
    var pollOptions by remember { mutableStateOf<List<String>>(listOf("", "")) }
    val context = LocalContext.current

    var pendingPreviewUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pendingPreviewType by remember { mutableStateOf("image") }

    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isNotEmpty()) {
            val firstMime = context.contentResolver.getType(uris.first())
            pendingPreviewType = if (firstMime?.startsWith("video") == true) "video" else "image"
            pendingPreviewUris = uris
        }
    }

    Dialog(
        onDismissRequest = onDismiss, 
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.8f).padding(bottom = 80.dp), 
                shape = RoundedCornerShape(24.dp), 
                color = MaterialTheme.colorScheme.surface, 
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), 
                        horizontalArrangement = Arrangement.SpaceBetween, 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface) }
                        Button(
                            onClick = {
                                val mediaList = ArrayList<String>()
                                for (u in selectedMediaUris) mediaList.add(u.toString())
                                for (u in selectedAudioUris) mediaList.add(u.toString())
                                
                                val postType = when { 
                                    showPoll -> "poll"
                                    replyingTo != null -> "reply"
                                    quotingPost != null -> "repost"
                                    else -> "standard"
                                }

                                val finalPoll = if (showPoll) pollOptions.filter { it.isNotBlank() } else null
                                onSubmit(content, mediaList, emptyList<String>(), postType, finalPoll, selectedLocation)
                            }, 
                            enabled = content.isNotEmpty() || selectedMediaUris.isNotEmpty() || selectedAudioUris.isNotEmpty() || showPoll, 
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary), 
                            shape = RoundedCornerShape(20.dp), 
                            modifier = Modifier.padding(end = 8.dp).height(36.dp)
                        ) { 
                            Text(if (replyingTo != null) "Reply" else "Post", fontWeight = FontWeight.Bold) 
                        }
                    }

                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                        if (replyingTo != null) {
                            Row(modifier = Modifier.padding(bottom = 8.dp)) {
                                Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                                    Box(modifier = Modifier.width(2.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Replying to @${replyingTo.authorHandle}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(avatarColor)) {
                                if (avatarUrl != null) { 
                                    AsyncImage(model = avatarUrl, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) 
                                } else { 
                                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.align(Alignment.Center)) 
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            TextField(
                                value = content, 
                                onValueChange = { content = it }, 
                                placeholder = { Text(if (replyingTo != null) "Post your reply" else "What's happening?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp) }, 
                                modifier = Modifier.fillMaxWidth(), 
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent, 
                                    unfocusedContainerColor = Color.Transparent, 
                                    cursorColor = MaterialTheme.colorScheme.primary, 
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ), 
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp)
                            )
                        }

                        if (selectedMediaUris.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(selectedMediaUris) { uri ->
                                    val mimeType = context.contentResolver.getType(uri)
                                    val isVideo = mimeType?.startsWith("video") == true
                                    Box(modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                        AsyncImage(model = uri, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        if (isVideo) { 
                                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)) 
                                        }
                                        IconButton(
                                            onClick = { selectedMediaUris = selectedMediaUris.filter { it != uri } }, 
                                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) { 
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp)) 
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedLocation != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp), 
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(selectedLocation!!, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp).clickable { selectedLocation = null })
                            }
                        }

                        if (showPoll) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(), 
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), 
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)), 
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    pollOptions.forEachIndexed { index, option ->
                                        OutlinedTextField(
                                            value = option, 
                                            onValueChange = { newValue -> 
                                                val newList = pollOptions.toMutableList()
                                                newList[index] = newValue
                                                pollOptions = newList 
                                            }, 
                                            placeholder = { Text("Choice ${index + 1}") }, 
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface, 
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                            ), 
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (quotingPost != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(quotingPost.authorAvatarColor)) {
                                            AsyncImage(model = quotingPost.authorAvatarUrl, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(quotingPost.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(" @${quotingPost.authorHandle}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(quotingPost.content, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    if (quotingPost.mediaUrls.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                            AsyncImage(model = quotingPost.mediaUrls[0], null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Footer toolbar
                    Column(modifier = Modifier.fillMaxWidth().imePadding()) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).clickable { }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Everyone can reply", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp), 
                            horizontalArrangement = Arrangement.Start, 
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { mediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }) { 
                                Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) 
                            }
                            IconButton(onClick = { 
                                if (!showPoll) { 
                                    showPoll = true
                                    selectedMediaUris = emptyList() 
                                    selectedAudioUris = emptyList() 
                                } 
                            }) { 
                                Icon(Icons.Default.Poll, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) 
                            }
                            IconButton(onClick = { selectedLocation = "Cape Town, SA" }) { 
                                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        if (pendingPreviewUris.isNotEmpty()) {
            com.tpsk.tj.chat.ui.screens.MediaPreviewOverlay(
                uris = pendingPreviewUris,
                type = pendingPreviewType,
                onDismiss = { pendingPreviewUris = emptyList() },
                onSend = { caption, processedUris ->
                    if (caption.isNotBlank() && content.isBlank()) {
                        content = caption
                    }
                    val current = selectedMediaUris.toMutableList()
                    for (uri in processedUris) {
                        if (!current.contains(uri)) current.add(uri)
                    }
                    selectedMediaUris = current
                    pendingPreviewUris = emptyList()
                }
            )
        }
    }
}
