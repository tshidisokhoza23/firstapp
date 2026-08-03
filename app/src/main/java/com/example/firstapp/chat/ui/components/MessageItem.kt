package com.example.firstapp.chat.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
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
    onMediaClick: (String, String) -> Unit = { _, _ -> }
) {
    val alignment = if (messageItem.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (messageItem.isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (messageItem.isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    
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
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
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
                            color = Color.Black.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (replyToSender == "You" || referencedMessage?.isFromMe == true) "You" else (replyToSender ?: "Sender"),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = replyToContent ?: "Message",
                                        color = contentColor.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Sender Name (For Groups)
                    if (!messageItem.isFromMe && messageItem.senderName.isNotEmpty() && messageItem.senderName != "Me") {
                        Text(
                            text = messageItem.senderName,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    // Content Rendering
                    if (messageItem.type == MessageType.VOICE_CALL || (messageItem.content != null && messageItem.content.contains("Voice call"))) {
                        CallLogItem(messageItem, contentColor)
                    } else {
                        if (messageItem.mediaUrls.isNotEmpty() ||
                            messageItem.type == MessageType.LOCATION || 
                            messageItem.type == MessageType.APPLICATION_LINK) {
                            AttachmentPreview(messageItem, contentColor, onMediaClick = onMediaClick)
                        }

                        val isMediaTypePlaceholder = messageItem.content == "Voice Note" || 
                                                   messageItem.content == "Image" || 
                                                   messageItem.content == "Video" || 
                                                   messageItem.content == "Shared Location" ||
                                                   messageItem.content == "File"

                        if (messageItem.content.isNotBlank() && (messageItem.type == MessageType.TEXT || !isMediaTypePlaceholder)) {
                            Text(
                                text = messageItem.content,
                                color = contentColor,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.35f).sp
                            )
                        }
                    }

                    // Timestamp & Status Ticks
                    Row(
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatWhatsAppTime(messageItem.timestamp),
                            color = contentColor.copy(alpha = 0.6f),
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
                                MessageStatus.READ -> if (showReadReceipts) Color(0xFF1D9BF0) else contentColor.copy(alpha = 0.4f)
                                MessageStatus.FAILED -> Color.Red
                                MessageStatus.SENDING, MessageStatus.PENDING -> contentColor.copy(alpha = 0.3f)
                                else -> contentColor.copy(alpha = 0.4f)
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
                        .padding(top = (-10).dp)
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
                .background(contentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isOutgoing) Icons.Default.CallMade else Icons.Default.CallReceived,
                contentDescription = null,
                tint = if (isMissed) Color.Red else MaterialTheme.colorScheme.primary,
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
                color = contentColor.copy(alpha = 0.7f),
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
    onMediaClick: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val firstMedia = message.mediaUrls.firstOrNull()?.replace("local://", "file://") ?: ""
    
    when (message.type) {
        MessageType.IMAGE -> {
            if (message.mediaUrls.size > 1) {
                MediaGrid(urls = message.mediaUrls) { url ->
                    onMediaClick(url, "image")
                }
            } else if (firstMedia.isNotEmpty()) {
                AsyncImage(
                    model = firstMedia,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onMediaClick(firstMedia, "image") },
                    contentScale = ContentScale.Crop
                )
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
                    model = firstMedia,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        MessageType.AUDIO_MESSAGE -> {
            VoiceNotePlayer(uri = firstMedia, duration = message.duration, isFromMe = message.isFromMe)
        }
        MessageType.DOCUMENT -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
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
                        color = contentColor.copy(alpha = 0.7f),
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
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    .clickable {
                        val gmmIntentUri = Uri.parse("geo:${message.latitude},${message.longitude}?q=${message.latitude},${message.longitude}")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Text("Shared Location", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    Text(text = message.contactName ?: "Contact", color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Work, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Job Application", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Profile Summary Attached", color = contentColor.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun VoiceNotePlayer(uri: String, duration: String?, isFromMe: Boolean = false) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPrepared by remember { mutableStateOf(false) }
    val mediaPlayer = remember { android.media.MediaPlayer() }

    val barHeights = remember { List(25) { (4..18).random() } }

    DisposableEffect(uri) {
        if (uri.isNotBlank()) {
            try {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(context, Uri.parse(uri))
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener { isPrepared = true }
                mediaPlayer.setOnCompletionListener { isPlaying = false; progress = 0f; mediaPlayer.seekTo(0) }
            } catch (_: Exception) { isPrepared = false }
        }
        onDispose { try { mediaPlayer.release() } catch (_: Exception) {} }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && isPrepared) {
                try {
                    if (mediaPlayer.duration > 0) {
                        progress = mediaPlayer.currentPosition.toFloat() / mediaPlayer.duration
                    }
                } catch (_: Exception) {}
                delay(100)
            }
        }
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
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(2.dp)
            )
        }

        IconButton(
            onClick = {
                if (!isPrepared) return@IconButton
                if (isPlaying) { mediaPlayer.pause(); isPlaying = false }
                else { mediaPlayer.start(); isPlaying = true }
            },
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                val barWidthPx = 3.dp.toPx()
                val spacingPx = 2.dp.toPx()
                val canvasWidth = size.width
                val canvasHeight = size.height
                val count = (canvasWidth / (barWidthPx + spacingPx)).toInt().coerceAtMost(barHeights.size)
                
                for (i in 0 until count) {
                    val barHeightPx = barHeights[i].dp.toPx()
                    val x = i * (barWidthPx + spacingPx)
                    val isPlayed = progress > (i.toFloat() / count.toFloat())
                    
                    drawLine(
                        color = if (isPlayed) AppAccent else AppTextSecondary.copy(alpha = 0.3f),
                        start = Offset(x, canvasHeight / 2 - barHeightPx / 2),
                        end = Offset(x, canvasHeight / 2 + barHeightPx / 2),
                        strokeWidth = barWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }
            Text(text = duration ?: "Voice", color = AppTextSecondary, fontSize = 10.sp)
        }
    }
}
