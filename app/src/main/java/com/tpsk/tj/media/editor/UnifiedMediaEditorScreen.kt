package com.tpsk.tj.media.editor

import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Title
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun UnifiedMediaEditorScreen(
    inputUri: Uri,
    mediaType: MediaType,
    destination: MediaDestination,
    onDismiss: () -> Unit,
    onResult: (MediaEditorResult) -> Unit,
    processor: MediaProcessor = DefaultMediaProcessor
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var caption by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(false) }
    var textOverlayInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Video Trimming state
    var videoDurationMs by remember { mutableLongStateOf(0L) }
    var trimStartMs by remember { mutableLongStateOf(0L) }
    var trimEndMs by remember { mutableLongStateOf(0L) }

    val limits = remember(destination) { MediaLimitsFactory.getLimitsFor(destination) }

    // File Size Validation (250 MB Cap)
    LaunchedEffect(inputUri) {
        val pfd = context.contentResolver.openFileDescriptor(inputUri, "r")
        val size = pfd?.statSize ?: 0L
        pfd?.close()

        if (size > limits.maxSizeBytes) {
            Toast.makeText(context, "File exceeds maximum limit of 250 MB", Toast.LENGTH_LONG).show()
            onDismiss()
        }
    }

    val is916Portrait = destination == MediaDestination.STORY || destination == MediaDestination.SHORT

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // MARK: - Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Text(
                    text = "Preview",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mediaType == MediaType.VIDEO) {
                        IconButton(onClick = { isMuted = !isMuted }) {
                            Icon(
                                if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(onClick = { /* Interactive Crop */ }) {
                        Icon(Icons.Default.Crop, contentDescription = "Crop", tint = Color.White)
                    }

                    IconButton(onClick = { /* Text Overlay */ }) {
                        Icon(Icons.Default.Title, contentDescription = "Text Overlay", tint = Color.White)
                    }
                }
            }

            // MARK: - Video Trimmer Slider Bar
            if (mediaType == MediaType.VIDEO && videoDurationMs > 0L) {
                val maxSelectionMs = limits.maxDurationMs ?: videoDurationMs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Slider(
                        value = trimEndMs.toFloat(),
                        onValueChange = { newVal ->
                            val end = newVal.toLong().coerceIn(trimStartMs + 1000L, (trimStartMs + maxSelectionMs).coerceAtMost(videoDurationMs))
                            trimEndMs = end
                        },
                        valueRange = 0f..videoDurationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF2563EB),
                            activeTrackColor = Color(0xFF2563EB),
                            inactiveTrackColor = Color.DarkGray
                        )
                    )

                    val durationSecs = ((trimEndMs - trimStartMs) / 1000L).coerceAtLeast(0L)
                    Text(
                        text = "Trim: ${durationSecs}s / Max ${(maxSelectionMs / 1000L)}s",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // MARK: - Main Media Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (is916Portrait) Modifier.aspectRatio(9f / 16f) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (mediaType == MediaType.VIDEO) {
                    var player by remember { mutableStateOf<ExoPlayer?>(null) }
                    DisposableEffect(inputUri) {
                        val exo = ExoPlayer.Builder(context).build().apply {
                            setMediaItem(MediaItem.fromUri(inputUri))
                            repeatMode = Player.REPEAT_MODE_ALL
                            prepare()
                            playWhenReady = true
                            addListener(object : Player.Listener {
                                override fun onPlaybackStateChanged(state: Int) {
                                    if (state == Player.STATE_READY) {
                                        val total = duration.coerceAtLeast(0L)
                                        videoDurationMs = total
                                        trimStartMs = 0L
                                        val cap = limits.maxDurationMs ?: total
                                        trimEndMs = if (total > cap) cap else total
                                    }
                                }
                            })
                        }
                        player = exo
                        onDispose {
                            exo.release()
                        }
                    }

                    LaunchedEffect(isMuted) {
                        player?.volume = if (isMuted) 0f else 1f
                    }

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                this.player = player
                            }
                        },
                        update = { view ->
                            view.player = player
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = inputUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = if (is916Portrait) ContentScale.Crop else ContentScale.Fit
                    )
                }

                if (textOverlayInput.isNotEmpty()) {
                    Text(
                        text = textOverlayInput,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }

            // MARK: - Bottom Caption & Send FAB Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    placeholder = { Text("Add a caption...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedContainerColor = Color(0xFF1E2228),
                        unfocusedContainerColor = Color(0xFF1E2228),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                FloatingActionButton(
                    onClick = {
                        if (isProcessing) return@FloatingActionButton
                        isProcessing = true
                        scope.launch {
                            val res = if (mediaType == MediaType.VIDEO) {
                                processor.trimAndCompressVideo(
                                    context = context,
                                    inputUri = inputUri,
                                    startMs = trimStartMs,
                                    endMs = if (trimEndMs > 0L) trimEndMs else videoDurationMs,
                                    destination = destination,
                                    caption = caption.ifBlank { null },
                                    onProgress = { progress = it }
                                )
                            } else {
                                processor.processImage(
                                    context = context,
                                    inputUri = inputUri,
                                    destination = destination,
                                    caption = caption.ifBlank { null }
                                )
                            }

                            isProcessing = false
                            res.onSuccess { result ->
                                onResult(result)
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Processing failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(52.dp)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
