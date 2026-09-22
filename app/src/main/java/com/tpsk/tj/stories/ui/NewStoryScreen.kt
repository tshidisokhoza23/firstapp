package com.tpsk.tj.stories.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tpsk.tj.common.util.VideoTrimmerUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewStoryScreen(
    onBack: () -> Unit,
    onNext: (content: String, url: String, type: String) -> Unit
) {
    val context = LocalContext.current
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("text") }
    var showTrimmerDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it)
            val isVideo = mimeType?.startsWith("video") == true
            
            selectedMediaUri = it
            mediaType = if (isVideo) "video" else "image"
        }
    }

    var videoDurationMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(selectedMediaUri) {
        if (selectedMediaUri != null && mediaType == "video") {
            videoDurationMs = com.tpsk.tj.common.util.MediaUtils.getVideoDuration(context, selectedMediaUri!!)
        } else {
            videoDurationMs = 0L
        }
    }

    val isVideoTooLong = mediaType == "video" && videoDurationMs > 120000L

    LaunchedEffect(Unit) {
        if (selectedMediaUri == null) {
            launcher.launch("*/*")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("New Story", color = Color.White)
                        if (isVideoTooLong) {
                            Text("Video exceeds 2m limit", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (selectedMediaUri != null && mediaType == "video") {
                        IconButton(onClick = { showTrimmerDialog = true }) {
                            Icon(Icons.Default.ContentCut, contentDescription = "Trim Video", tint = if (isVideoTooLong) Color.Yellow else Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Add a caption...", color = Color.LightGray) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (mediaType == "video") {
                        OutlinedButton(
                            onClick = { showTrimmerDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.ContentCut, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Trim Video")
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    
                    Button(
                        onClick = { 
                            if (isVideoTooLong) {
                                showTrimmerDialog = true
                            } else {
                                onNext(caption, selectedMediaUri?.toString() ?: "", mediaType)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVideoTooLong) Color.Yellow else MaterialTheme.colorScheme.primary,
                            contentColor = if (isVideoTooLong) Color.Black else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(if (isVideoTooLong) "TRIM REQUIRED" else "NEXT", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedMediaUri != null) {
                AsyncImage(
                    model = selectedMediaUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                if (isVideoTooLong) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Warning, null, tint = Color.Yellow, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Video is too long", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("${videoDurationMs / 1000}s selected (Max 120s)", color = Color.LightGray, fontSize = 14.sp)
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = { showTrimmerDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.ContentCut, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("TRIM VIDEO NOW")
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF8D6E63), Color(0xFF4E342E)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select media or start typing...", color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }

    if (showTrimmerDialog && selectedMediaUri != null) {
        VideoTrimmerDialog(
            videoUri = selectedMediaUri!!,
            totalDurationMs = videoDurationMs,
            onDismiss = { showTrimmerDialog = false },
            onTrimmed = { trimmedUri ->
                selectedMediaUri = trimmedUri
                showTrimmerDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun VideoTrimmerDialog(
    videoUri: Uri,
    totalDurationMs: Long,
    onDismiss: () -> Unit,
    onTrimmed: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }

    val maxAllowedMs = 120_000L // 2 minutes
    var rangeValues by remember(totalDurationMs) {
        val initialEnd = totalDurationMs.coerceAtMost(maxAllowedMs).toFloat()
        mutableStateOf(0f..initialEnd.coerceAtLeast(1000f))
    }

    val startMs = rangeValues.start.toLong()
    val endMs = rangeValues.endInclusive.toLong()
    val selectedDurationMs = endMs - startMs

    val exoPlayer = remember(videoUri) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUri))
            prepare()
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(startMs, endMs, exoPlayer) {
        exoPlayer.seekTo(startMs)
        while (isActive) {
            delay(100)
            if (exoPlayer.currentPosition >= endMs || exoPlayer.currentPosition < startMs) {
                exoPlayer.seekTo(startMs)
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isTrimming) onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "Trim Video Story",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            androidx.media3.ui.PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Start: ${VideoTrimmerUtil.formatDurationMs(startMs)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "End: ${VideoTrimmerUtil.formatDurationMs(endMs)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(8.dp))

                RangeSlider(
                    value = rangeValues,
                    onValueChange = { newRange ->
                        val duration = newRange.endInclusive - newRange.start
                        if (duration <= maxAllowedMs && duration >= 1000f) {
                            rangeValues = newRange
                        } else if (duration > maxAllowedMs) {
                            val newEnd = (newRange.start + maxAllowedMs).coerceAtMost(totalDurationMs.toFloat())
                            rangeValues = newRange.start..newEnd
                        }
                    },
                    valueRange = 0f..totalDurationMs.coerceAtLeast(1000L).toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Trimmed Length: ${VideoTrimmerUtil.formatDurationMs(selectedDurationMs)} (Max 2:00)",
                    fontSize = 12.sp,
                    color = if (selectedDurationMs > maxAllowedMs) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (isTrimming) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Trimming video...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isTrimming && selectedDurationMs > 0 && selectedDurationMs <= maxAllowedMs,
                onClick = {
                    isTrimming = true
                    scope.launch {
                        try {
                            val trimmedUri = VideoTrimmerUtil.trimVideo(
                                context = context,
                                inputUri = videoUri,
                                startMs = startMs,
                                endMs = endMs
                            )
                            onTrimmed(trimmedUri)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Trim failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                        } finally {
                            isTrimming = false
                        }
                    }
                }
            ) {
                Text("APPLY TRIM")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isTrimming,
                onClick = onDismiss
            ) {
                Text("CANCEL")
            }
        }
    )
}
