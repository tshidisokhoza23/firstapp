package com.tpsk.tj.media.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawWithContent
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
    var selectedVisibility by remember { mutableStateOf("public") }
    var selectedCircleId by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("") }
    var scheduledAt by remember { mutableStateOf<Long?>(null) }
    var collaborators by remember { mutableStateOf(listOf<com.tpsk.tj.common.data.api.PersonDto>()) }
    var circles by remember { mutableStateOf(listOf<com.tpsk.tj.common.data.api.CircleDto>()) }
    var showAudience by remember { mutableStateOf(false) }
    var showSchedule by remember { mutableStateOf(false) }
    var showCollaborators by remember { mutableStateOf(false) }
    var showMood by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var locationDraft by remember { mutableStateOf("") }
    var peopleQuery by remember { mutableStateOf("") }
    var people by remember { mutableStateOf(listOf<com.tpsk.tj.common.data.api.PersonDto>()) }
    var voicePath by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf(false) }
    var draftLabel by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    var recorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) draftLabel = "Microphone permission is needed for a voice note"
    }

    LaunchedEffect(Unit) {
        mediaViewModel.loadDraft { draft ->
            if (postText.isBlank() && draft.content.isNotBlank()) {
                postText = draft.content
                selectedMood = draft.mood.orEmpty()
                selectedVisibility = draft.audience.ifBlank { "public" }
                selectedCircleId = draft.circleId.orEmpty()
                draftLabel = "Draft restored"
            }
        }
    }

    LaunchedEffect(postText, selectedMood, selectedVisibility) {
        if (postText.isBlank()) return@LaunchedEffect
        kotlinx.coroutines.delay(800)
        draftLabel = "Saving draft"
        mediaViewModel.saveDraft(
            com.tpsk.tj.common.data.api.ComposeMetaDto(
                content = postText,
                mood = selectedMood,
                audience = selectedVisibility,
                circleId = selectedCircleId
            )
        )
        draftLabel = "Draft saved"
    }

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
        colors = listOf(Color(0xFFA855F7), Color(0xFF14B8A6))
    )
    var artistTags by remember { mutableStateOf(setOf<String>()) }

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
                            color = if (remaining < 0) Color(0xFFEF4444) else Color(0xFF14B8A6)
                        )
                        Text(
                            text = draftLabel.ifBlank { " " },
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (!canPost || isPostingState) return@Button
                            isPostingState = true

                            val imageUriStrings = selectedImageUris.map { it.toString() } + listOfNotNull(voicePath)
                            val postType = when {
                                showPoll -> "poll"
                                replyToPost != null -> "reply"
                                quotingPost != null -> "repost"
                                else -> "standard"
                            }
                            val finalPoll = if (showPoll) pollOptions.filter { it.isNotBlank() } else null

                            val artistLine = artistTags.joinToString(" · ")
                            val content = if (artistLine.isBlank() || postText.contains(artistLine)) postText else "$postText\n$artistLine"
                            mediaViewModel.createPost(
                                content = content,
                                mediaUris = imageUriStrings,
                                postType = postType,
                                pollOptions = finalPoll,
                                location = selectedLocation,
                                parentPostId = replyToPost?.id,
                                rootPostId = replyToPost?.rootPostId ?: replyToPost?.id,
                                repostOfId = quotingPost?.id,
                                compose = com.tpsk.tj.common.data.api.ComposeMetaDto(
                                    content = content,
                                    mood = selectedMood,
                                    audience = selectedVisibility,
                                    circleId = selectedCircleId,
                                    scheduledAt = scheduledAt,
                                    aiAssisted = isAiImproved,
                                    collaborators = collaborators.map { it.id },
                                    splits = if (collaborators.isEmpty()) emptyList() else {
                                        val each = 100 / (collaborators.size + 1)
                                        collaborators.map { com.tpsk.tj.common.data.api.SplitInputDto(it.id, each.coerceAtLeast(1)) }
                                    }
                                )
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
                            .clickable {
                                mediaViewModel.loadCircles { circles = it }
                                showAudience = true
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF14B8A6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (selectedVisibility) {
                                "followers" -> "Followers ▾"
                                "circle" -> "Circle ▾"
                                else -> "Public ▾"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF14B8A6)
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
                            Icon(Icons.Outlined.Image, contentDescription = "Image", tint = Color(0xFF14B8A6))
                        }

                        IconButton(
                            onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.SingleMimeType("video/*"))
                                )
                            }
                        ) {
                            Icon(Icons.Outlined.Videocam, contentDescription = "Video", tint = Color(0xFF14B8A6))
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
                            Icon(Icons.Outlined.Poll, contentDescription = "Poll", tint = Color(0xFF14B8A6))
                        }

                        IconButton(onClick = {
                            pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.SingleMimeType("image/gif")))
                        }) {
                            Text("GIF", color = Color(0xFF14B8A6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        IconButton(onClick = {
                            if (!recording && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                                return@IconButton
                            }
                            if (recording) {
                                runCatching { recorder?.stop(); recorder?.release() }
                                recorder = null
                                recording = false
                            } else {
                                val file = java.io.File(context.cacheDir, "post_voice_${System.currentTimeMillis()}.m4a")
                                val next = android.media.MediaRecorder()
                                runCatching {
                                    next.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                                    next.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                                    next.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                                    next.setOutputFile(file.absolutePath)
                                    next.prepare()
                                    next.start()
                                    recorder = next
                                    voicePath = file.absolutePath
                                    recording = true
                                }.onFailure { next.release() }
                            }
                        }) {
                            Icon(if (recording) Icons.Default.Stop else Icons.Outlined.Mic, contentDescription = "Voice note", tint = Color(0xFF14B8A6))
                        }

                        IconButton(onClick = {
                            locationDraft = selectedLocation.orEmpty()
                            showLocationDialog = true
                        }) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = "Location", tint = Color(0xFF14B8A6))
                        }

                        // AI Assist Sparkle Button
                        IconButton(onClick = {
                            val image = selectedImageUris.firstOrNull()?.toString()
                            if (image == null) return@IconButton
                            mediaViewModel.artistCaption(image) { caption ->
                                if (caption.isNotBlank()) postText = caption
                            }
                        }) {
                            Icon(Icons.Default.Star, "AI caption", tint = Color(0xFF8A2BE2))
                        }
                        var showGif by remember { mutableStateOf(false) }
                        IconButton(onClick = { showGif = true }) {
                            Icon(Icons.Default.Image, "GIF", tint = Color(0xFF8A2BE2))
                        }
                        if (showGif) {
                            var query by remember { mutableStateOf("") }
                            var gifs by remember { mutableStateOf(listOf<com.tpsk.tj.common.data.api.GifDto>()) }
                            var gifError by remember { mutableStateOf<String?>(null) }
                            AlertDialog(
                                onDismissRequest = { showGif = false },
                                title = { Text("GIFs") },
                                text = {
                                    Column {
                                        OutlinedTextField(query, { query = it }, label = { Text("Search") })
                                        gifError?.let { Text(it, color = Color(0xFFFF6B6B)) }
                                        gifs.forEach { gif ->
                                            TextButton(onClick = {
                                                postText = listOf(postText, gif.url).filter { it.isNotBlank() }.joinToString("\n")
                                                showGif = false
                                            }) { Text(gif.title.ifBlank { gif.url }) }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { mediaViewModel.searchGifs(query) { found, error -> gifs = found; gifError = error } }) { Text("Search") }
                                },
                                dismissButton = { TextButton(onClick = { showGif = false }) { Text("Close") } }
                            )
                        }
                        IconButton(onClick = { showAiAssistSheet = true }) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Assist",
                                tint = Color(0xFF14B8A6)
                            )
                        }

                        IconButton(onClick = { showSchedule = true }) {
                            Icon(Icons.Outlined.Schedule, contentDescription = "Schedule", tint = Color(0xFF14B8A6))
                        }

                        IconButton(onClick = { showCollaborators = true }) {
                            Icon(Icons.Outlined.Group, contentDescription = "Collaborators", tint = Color(0xFF14B8A6))
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
                        .border(3.dp, gradientBrush, CircleShape)
                        .drawWithContent {
                            drawContent()
                            val sweep = (postText.length.coerceIn(0, charLimit) / charLimit.toFloat()) * 360f
                            drawArc(
                                color = Color(0xFF14B8A6),
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                            )
                        }
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
                        cursorBrush = SolidColor(Color(0xFF14B8A6)),
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
                        border = BorderStroke(1.dp, Color(0xFF14B8A6).copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { showMood = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (selectedMood.isBlank()) "Add a mood" else selectedMood,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF14B8A6)
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
                            color = Color(0xFF14B8A6),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (currentUser?.isArtist == true) {
                Text(
                    text = "ARTIST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF71717A),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Add to Portfolio", "Process / WIP", "Part of Series", "Open for Commissions")) { tag ->
                        val selected = artistTags.contains(tag)
                        Surface(
                            onClick = {
                                artistTags = if (selected) artistTags - tag else artistTags + tag
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) Color(0xFF14B8A6) else Color(0xFF2A2A2A),
                            border = BorderStroke(1.dp, Color(0xFF333333))
                        ) {
                            Text(
                                text = tag,
                                color = if (selected) Color.White else Color(0xFF14B8A6),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
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
    if (showAudience) {
        AlertDialog(
            onDismissRequest = { showAudience = false },
            title = { Text("Who can see this") },
            text = {
                Column {
                    TextButton(onClick = { selectedVisibility = "public"; selectedCircleId = ""; showAudience = false }) { Text("Public") }
                    TextButton(onClick = { selectedVisibility = "followers"; selectedCircleId = ""; showAudience = false }) { Text("Followers only") }
                    circles.forEach { circle ->
                        TextButton(onClick = {
                            selectedVisibility = "circle"
                            selectedCircleId = circle.id
                            showAudience = false
                        }) { Text(circle.name.ifBlank { "Circle" }) }
                    }
                    if (circles.isEmpty()) Text("Create a circle from the social menu to post to one.", color = Color.Gray, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showAudience = false }) { Text("Close") } }
        )
    }
    if (showMood) {
        val moods = listOf("Feeling creative", "Hot take", "Question", "Excited", "Grateful")
        AlertDialog(
            onDismissRequest = { showMood = false },
            title = { Text("Mood") },
            text = {
                Column {
                    moods.forEach { mood ->
                        TextButton(onClick = { selectedMood = mood; showMood = false }) { Text(mood) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedMood = ""; showMood = false }) { Text("Clear") } }
        )
    }
    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Location") },
            text = {
                OutlinedTextField(value = locationDraft, onValueChange = { locationDraft = it }, label = { Text("Place") })
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedLocation = locationDraft.trim().ifBlank { null }
                    showLocationDialog = false
                }) { Text("Save") }
            }
        )
    }
    if (showSchedule) {
        AlertDialog(
            onDismissRequest = { showSchedule = false },
            title = { Text("Schedule") },
            text = { Text(if (scheduledAt == null) "Publish one hour from now, or clear the schedule." else "Scheduled.") },
            confirmButton = {
                TextButton(onClick = {
                    scheduledAt = System.currentTimeMillis() + 60 * 60 * 1000
                    showSchedule = false
                }) { Text("In 1 hour") }
            },
            dismissButton = {
                TextButton(onClick = { scheduledAt = null; showSchedule = false }) { Text("Post now") }
            }
        )
    }
    if (showCollaborators) {
        AlertDialog(
            onDismissRequest = { showCollaborators = false },
            title = { Text("Collaborators") },
            text = {
                Column {
                    OutlinedTextField(
                        value = peopleQuery,
                        onValueChange = {
                            peopleQuery = it
                            mediaViewModel.searchPeople(it) { found -> people = found }
                        },
                        label = { Text("Search people") }
                    )
                    people.forEach { person ->
                        TextButton(onClick = {
                            if (collaborators.none { it.id == person.id }) collaborators = collaborators + person
                        }) { Text(person.handle.ifBlank { person.name }) }
                    }
                    if (collaborators.isNotEmpty()) {
                        Text("Added: " + collaborators.joinToString { it.handle.ifBlank { it.name } }, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCollaborators = false }) { Text("Done") } }
        )
    }
    if (showAiAssistSheet) {
        val repo = aiRepository ?: remember { AiRepository(null) }
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
