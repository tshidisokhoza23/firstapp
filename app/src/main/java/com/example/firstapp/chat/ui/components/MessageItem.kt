package com.example.firstapp.chat.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import android.util.Patterns
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.firstapp.media.ui.components.MediaGrid
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.MessageStatus
import com.example.firstapp.chat.model.MessageType
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.formatWhatsAppTime
import kotlin.collections.*
import kotlin.text.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue

fun getSenderColor(senderId: String): Color {
    val colors = listOf(
        Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF7E57C2),
        Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF29B6F6), Color(0xFF26C6DA),
        Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFD4E157),
        Color(0xFFFFEE58), Color(0xFFFFCA28), Color(0xFFFFA726), Color(0xFFFF7043)
    )
    if (senderId.isEmpty()) return Color.Gray
    return colors[senderId.hashCode().absoluteValue % colors.size]
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageItemUnique(
    messageItem: Message,
    isSelected: Boolean,
    referencedMessage: Message? = null,
    fontSize: Float = 16f,
    showReadReceipts: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onReply: (Message) -> Unit,
    onMediaClick: (String, String) -> Unit = { _, _ -> },
    playingAudioUri: String? = null,
    onPlayAudio: (String?) -> Unit = {},
    isHighlighted: Boolean = false
) {
    val alignment = if (messageItem.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (messageItem.isFromMe) MaterialTheme.colorScheme.primary else Color.White
    val contentColor = if (messageItem.isFromMe) Color.White else Color.Black
    
    // Unique rounded bubble shape without tails
    val shape = if (messageItem.isFromMe) {
        RoundedCornerShape(18.dp, 18.dp, 2.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 2.dp)
    }

    val dragAmount = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, drag ->
                        val newValue = (dragAmount.value + drag)
                        if (!messageItem.isFromMe && newValue > 0) {
                            scope.launch { dragAmount.snapTo(newValue.coerceIn(0f, 100f)) }
                        } else if (messageItem.isFromMe && newValue < 0) {
                            scope.launch { dragAmount.snapTo(newValue.coerceIn(-100f, 0f)) }
                        }
                    },
                    onDragEnd = {
                        if (abs(dragAmount.value) > 60f) {
                            onReply(messageItem)
                        }
                        scope.launch { dragAmount.animateTo(0f) }
                    },
                    onDragCancel = { scope.launch { dragAmount.animateTo(0f) } }
                )
            }
            .background(
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    isHighlighted -> Color.Yellow.copy(alpha = 0.3f)
                    else -> Color.Transparent
                }
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Reply Icon Indicator (Dynamic highlight)
        if (dragAmount.value != 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = if (dragAmount.value > 0) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Reply,
                    null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = (abs(dragAmount.value) / 100f).coerceIn(0f, 1f)),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .align(alignment)
                .offset(x = dragAmount.value.dp)
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            Surface(
                shape = shape,
                color = if (isSelected) bubbleColor.copy(alpha = 0.8f) else bubbleColor,
                shadowElevation = 2.dp,
                border = if (!messageItem.isFromMe) BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) else null
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Reply / Referenced Message View
                    val replyToSender = referencedMessage?.senderName ?: messageItem.replyToSenderName
                    val replyToContent = referencedMessage?.content ?: messageItem.replyToText
                    
                    if (messageItem.replyToId != null || referencedMessage != null) {
                        Surface(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .fillMaxWidth(),
                            color = if (messageItem.isFromMe) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(36.dp)
                                        .clip(CircleShape)
                                        .background(if (messageItem.isFromMe) Color.White else MaterialTheme.colorScheme.primary)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (replyToSender == "You" || referencedMessage?.isFromMe == true) "You" else (replyToSender ?: "Sender"),
                                        color = if (messageItem.isFromMe) Color.White else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = replyToContent ?: "Message",
                                        color = if (messageItem.isFromMe) Color.White.copy(alpha = 0.9f) else Color.DarkGray,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Sender Name (For Groups/Channels)
                    if (!messageItem.isFromMe && messageItem.senderName.isNotEmpty() && messageItem.senderName != "Me") {
                        Text(
                            text = messageItem.senderName,
                            color = getSenderColor(messageItem.senderId),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 2.dp).clickable {
                                // Potentially navigate to sender profile
                            }
                        )
                    }

                    // Content Rendering
                    if (messageItem.type == MessageType.VOICE_CALL || (messageItem.content != null && messageItem.content.contains("Voice call"))) {
                        CallLogItem(messageItem, contentColor)
                    } else {
                        val rawMediaCheck = messageItem.mediaUrls.firstOrNull()
                            ?: if (messageItem.content.startsWith("http") || messageItem.content.startsWith("file:") || messageItem.content.startsWith("content:") || messageItem.content.startsWith("uploads/") || messageItem.content.startsWith("/")) messageItem.content
                            else if (!messageItem.fileName.isNullOrBlank() && messageItem.type != MessageType.TEXT) "chat/${messageItem.fileName}"
                            else ""

                        if (messageItem.mediaUrls.isNotEmpty() ||
                            rawMediaCheck.isNotEmpty() ||
                            messageItem.type == MessageType.IMAGE ||
                            messageItem.type == MessageType.VIDEO ||
                            messageItem.type == MessageType.AUDIO_MESSAGE ||
                            messageItem.type == MessageType.DOCUMENT ||
                            messageItem.type == MessageType.LOCATION || 
                            messageItem.type == MessageType.APPLICATION_LINK) {
                            AttachmentPreview(
                                message = messageItem, 
                                contentColor = contentColor, 
                                onMediaClick = onMediaClick,
                                playingAudioUri = playingAudioUri,
                                onPlayAudio = onPlayAudio
                            )
                        }

                        val isMediaTypePlaceholder = messageItem.content == "Voice Note" || 
                                                   messageItem.content == "Image" || 
                                                   messageItem.content == "Video" || 
                                                   messageItem.content == "Shared Location" ||
                                                   messageItem.content == "File"

                        if (messageItem.content.isNotBlank() && (messageItem.type == MessageType.TEXT || !isMediaTypePlaceholder)) {
                            Column {
                                SelectionContainer {
                                    ClickableLinkText(
                                        text = messageItem.content,
                                        color = contentColor,
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * 1.35f).sp
                                    )
                                }
                                
                                val preview = messageItem.linkPreview
                                if (preview != null) {
                                    Spacer(Modifier.height(8.dp))
                                    LinkPreviewCard(preview, messageItem.isFromMe)
                                }
                            }
                        }
                    }

                    // Timestamp & Status Ticks
                    Row(
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (messageItem.isStarred) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (messageItem.isFromMe) Color.White else Color(0xFFFFD700),
                                modifier = Modifier.size(12.dp).padding(end = 4.dp)
                            )
                        }
                        Text(
                            text = formatWhatsAppTime(messageItem.timestamp),
                            color = contentColor.copy(alpha = 0.8f),
                            fontSize = 10.sp
                        )
                        if (messageItem.isFromMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val tickIcon = when (messageItem.status) {
                                MessageStatus.READ -> Icons.Default.DoneAll
                                MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                MessageStatus.SENT -> Icons.Default.Done
                                MessageStatus.SENDING, MessageStatus.PENDING -> Icons.Default.AccessTime
                                MessageStatus.FAILED -> Icons.Default.Error
                            }
                            val tickColor = when (messageItem.status) {
                                MessageStatus.READ -> if (showReadReceipts) Color(0xFF81D4FA) else Color.White.copy(alpha = 0.6f)
                                MessageStatus.FAILED -> Color.Red
                                MessageStatus.SENDING, MessageStatus.PENDING -> Color.White.copy(alpha = 0.4f)
                                else -> Color.White.copy(alpha = 0.6f)
                            }
                            Icon(
                                imageVector = tickIcon,
                                contentDescription = messageItem.status.name,
                                tint = tickColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            
            // Reactions (Bubble-floating style)
            if (messageItem.reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .offset(y = (-10).dp)
                        .align(if (messageItem.isFromMe) Alignment.End else Alignment.Start),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    messageItem.reactions.entries.forEach { entry ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape,
                            shadowElevation = 4.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = entry.key, 
                                fontSize = 12.sp, 
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallLogItem(message: Message, contentColor: Color = MaterialTheme.colorScheme.onSurface) {
    val isMissed = message.content.startsWith("No answer")
    val isOutgoing = message.isFromMe
    val iconColor = if (message.isFromMe) Color.White else (if (isMissed) Color.Red else MaterialTheme.colorScheme.primary)
    
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isOutgoing) Icons.Default.CallMade else Icons.Default.CallReceived,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Voice call",
                color = contentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (isMissed) "No answer" else (message.duration ?: "Ended"),
                color = contentColor.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttachmentPreview(
    message: Message,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onMediaClick: (String, String) -> Unit = { _, _ -> },
    playingAudioUri: String? = null,
    onPlayAudio: (String?) -> Unit = {}
) {
    val context = LocalContext.current
    val rawFirstMedia = message.mediaUrls.firstOrNull()
        ?: if (message.content.startsWith("http") || message.content.startsWith("file:") || message.content.startsWith("content:") || message.content.startsWith("uploads/") || message.content.startsWith("/")) message.content
        else if (!message.fileName.isNullOrBlank() && message.type != MessageType.TEXT) "chat/${message.fileName}"
        else ""
    val firstMedia = com.example.firstapp.common.util.MediaUtils.formatUrl(rawFirstMedia) ?: ""
    
    when (message.type) {
        MessageType.IMAGE -> {
            if (message.mediaUrls.size > 1) {
                MediaGrid(
                    urls = message.mediaUrls,
                    autoplayEnabled = false,
                    autoplayGifsEnabled = false
                ) { url ->
                    onMediaClick(com.example.firstapp.common.util.MediaUtils.formatUrl(url) ?: url, "image")
                }
            } else if (firstMedia.isNotEmpty()) {
                AsyncImage(
                    model = firstMedia,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray.copy(alpha = 0.5f))
                        .clickable { onMediaClick(firstMedia, "image") },
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.graphics.painter.ColorPainter(Color.Red.copy(alpha = 0.2f)),
                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color.Gray.copy(alpha = 0.2f))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📷 Photo", color = contentColor, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        MessageType.VIDEO -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { onMediaClick(firstMedia, "video") },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(firstMedia)
                        .videoFrameMillis(1000)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = androidx.compose.ui.graphics.painter.ColorPainter(Color.DarkGray)
                )
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = AppAccent, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        MessageType.AUDIO_MESSAGE -> {
            VoiceNotePlayer(
                uri = firstMedia, 
                duration = message.duration, 
                isFromMe = message.isFromMe,
                isPlayingGlobal = playingAudioUri == firstMedia,
                onPlay = { onPlayAudio(if (it) firstMedia else null) },
                amplitudes = message.audioAmplitudes
            )
        }
        MessageType.DOCUMENT -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(contentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Description, 
                    null, 
                    tint = if (message.isFromMe) Color.White else MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.fileName ?: "Document",
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = message.fileSize ?: "File",
                        color = contentColor.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
        MessageType.LOCATION -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(contentColor.copy(alpha = 0.15f))
                    .clickable {
                        val gmmIntentUri = Uri.parse("geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.LocationOn, 
                    null, 
                    tint = if (message.isFromMe) Color.White else MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    "Shared Location", 
                    color = contentColor, 
                    fontSize = 14.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
        MessageType.CONTACT -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = message.contactName?.takeIf { it.isNotBlank() } ?: message.contactPhone ?: "", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = message.contactPhone ?: "", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${message.contactPhone}"))
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        MessageType.APPLICATION_LINK -> {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BusinessCenter, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val title = message.jobDetails?.title ?: "Job Application"
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(text = "Profile Summary Attached", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    }
                    
                    if (message.applicantProfile != null) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Applicant: ${message.applicantProfile.full_name}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (!message.applicantProfile.bio.isNullOrBlank()) {
                            Text(
                                text = message.applicantProfile.bio,
                                color = contentColor.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        MessageType.JOB_POST -> {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkOutline, null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Text("New Job Opportunity", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message.content, 
                        fontSize = 13.sp, 
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        maxLines = 3, 
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { /* Navigate to job */ },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("View Details", fontSize = 12.sp)
                    }
                }
            }
        }
        else -> {}
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VoiceNotePlayer(
    uri: String, 
    duration: String?, 
    isFromMe: Boolean = false,
    isPlayingGlobal: Boolean = false,
    onPlay: (Boolean) -> Unit = {},
    amplitudes: List<Int>? = null
) {
    val context = LocalContext.current
    var progress by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableLongStateOf(0L) }
    
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(Unit) {
        onDispose { 
            exoPlayer.release()
        }
    }

    LaunchedEffect(uri) {
        if (uri.isNotBlank()) {
            val formattedUri = if (!uri.startsWith("http") && !uri.startsWith("content") && !uri.startsWith("file")) {
                com.example.firstapp.chat.data.repository.formatUrl(uri)
            } else uri
            
            android.util.Log.d("VoiceNotePlayer", "Setting URI: $formattedUri")
            val mediaItem = androidx.media3.common.MediaItem.fromUri(formattedUri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        }
    }

    LaunchedEffect(isPlayingGlobal) {
        if (isPlayingGlobal) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Progress and completion tracking
    LaunchedEffect(exoPlayer) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) {
                    onPlay(false)
                    exoPlayer.seekTo(0)
                    progress = 0f
                }
                if (state == androidx.media3.common.Player.STATE_READY) {
                    durationMs = exoPlayer.duration
                }
            }
        }
        exoPlayer.addListener(listener)
        
        while (true) {
            if (exoPlayer.isPlaying) {
                val dur = exoPlayer.duration
                if (dur > 0) {
                    progress = exoPlayer.currentPosition.toFloat() / dur
                }
            }
            delay(100)
        }
    }

    val barHeights = remember(amplitudes) { 
        amplitudes ?: List(25) { (4..18).random() } 
    }

    Row(
        modifier = Modifier.width(260.dp).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))) {
                Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            }
            Icon(
                Icons.Default.Mic,
                null,
                tint = AppAccent,
                modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).background(Color.White, CircleShape).padding(2.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Interactive waveform-like progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .pointerInput(durationMs) {
                        detectTapGestures { offset ->
                            if (durationMs > 0) {
                                val ratio = offset.x / size.width
                                exoPlayer.seekTo((ratio * durationMs).toLong())
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Background bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    barHeights.forEach { height ->
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(height.dp)
                                .background(if (isFromMe) Color.White.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.3f))
                        )
                    }
                }
                
                // Progress mask/fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RectangleShape),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(1f / progress.coerceAtLeast(0.01f)),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        barHeights.forEach { height ->
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(height.dp)
                                    .background(if (isFromMe) Color.White else AppAccent)
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (duration != null && duration.length < 10) duration else "Voice",
                    color = if (isFromMe) Color.White.copy(alpha = 0.8f) else Color.Gray,
                    fontSize = 11.sp
                )
                
                if (isPlayingGlobal) {
                    Text(
                        text = "Playing",
                        color = if (isFromMe) Color.White else AppAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        IconButton(
            onClick = { onPlay(!isPlayingGlobal) },
            modifier = Modifier.size(36.dp).background(if (isFromMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(
                imageVector = if (isPlayingGlobal) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlayingGlobal) "Pause" else "Play",
                tint = if (isFromMe) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LinkPreviewCard(preview: com.example.firstapp.chat.model.LinkPreview, isFromMe: Boolean) {
    val backgroundColor = if (isFromMe) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    val contentColor = if (isFromMe) Color.White else MaterialTheme.colorScheme.onSurface
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(preview.url))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.1f))
    ) {
        Column {
            if (preview.thumbnailUrl != null) {
                AsyncImage(
                    model = preview.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                if (preview.siteName != null) {
                    Text(
                        text = preview.siteName,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                if (preview.title != null) {
                    Text(
                        text = preview.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (preview.description != null) {
                    Text(
                        text = preview.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ClickableLinkText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val annotatedString = buildAnnotatedString {
        // 1. Parse Markdown & Links
        val markdownRegex = Regex("(\\*\\*?.*?\\*\\*?|__?.*?__?|~~.*?~~|`.*?`|https?://\\S+|www\\.\\S+)")
        var lastIndex = 0
        
        val matches = markdownRegex.findAll(text)
        for (match in matches) {
            // Append text before the match
            append(text.substring(lastIndex, match.range.first))
            
            val matchText = match.value
            when {
                // Bold: **text** or *text*
                matchText.startsWith("**") && matchText.endsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append(matchText.substring(2, matchText.length - 2))
                    }
                }
                matchText.startsWith("*") && matchText.endsWith("*") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(matchText.substring(1, matchText.length - 1))
                    }
                }
                // Italic: _text_
                matchText.startsWith("_") && matchText.endsWith("_") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(matchText.substring(1, matchText.length - 1))
                    }
                }
                // Strikethrough: ~~text~~
                matchText.startsWith("~~") && matchText.endsWith("~~") -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(matchText.substring(2, matchText.length - 2))
                    }
                }
                // Code: `text`
                matchText.startsWith("`") && matchText.endsWith("`") -> {
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.Black.copy(alpha = 0.1f),
                        color = if (color == Color.White) Color.White else Color(0xFFE91E63)
                    )) {
                        append(matchText.substring(1, matchText.length - 1))
                    }
                }
                // Links
                matchText.startsWith("http") || matchText.startsWith("www.") -> {
                    val link = matchText
                    pushStringAnnotation(tag = "URL", annotation = link)
                    withStyle(
                        style = SpanStyle(
                            color = if (color == Color.White) Color.White else Color(0xFF2196F3),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(link)
                    }
                    pop()
                }
                else -> append(matchText)
            }
            lastIndex = match.range.last + 1
        }
        append(text.substring(lastIndex))
    }

    ClickableText(
        text = annotatedString,
        style = LocalTextStyle.current.copy(
            color = color,
            fontSize = fontSize,
            lineHeight = lineHeight
        ),
        modifier = modifier,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val url = if (!annotation.item.startsWith("http")) "https://${annotation.item}" else annotation.item
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("ClickableLinkText", "Failed to open link: $url", e)
                    }
                }
        }
    )
}
