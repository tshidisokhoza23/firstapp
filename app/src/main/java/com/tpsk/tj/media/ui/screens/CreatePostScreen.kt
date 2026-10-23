@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.tpsk.tj.media.ui.screens

import android.content.Intent
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import com.tpsk.tj.ui.navigation.Screen
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tpsk.tj.LocalCurrentUser
import com.tpsk.tj.common.data.repository.AiRepository
import com.tpsk.tj.common.data.repository.rememberAiRepository
import com.tpsk.tj.common.ui.components.AppFilterChip
import com.tpsk.tj.common.ui.theme.LocalIsDarkTheme
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.media.ui.components.AiAssistSheet
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import com.tpsk.tj.ui.screens.contest.ContestViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val isDark = LocalIsDarkTheme.current

    var postText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Technology") }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
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
    var showGifPicker by remember { mutableStateOf(false) }
    var gifQuery by remember { mutableStateOf("") }
    var gifResults by remember { mutableStateOf(listOf<com.tpsk.tj.common.data.api.GifDto>()) }
    var gifError by remember { mutableStateOf<String?>(null) }
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
    val maxMedia = 10
    val filledPollOptions = pollOptions.count { it.isNotBlank() }
    val canPost = (
        postText.isNotBlank() ||
            selectedImageUris.isNotEmpty() ||
            voicePath != null ||
            (showPoll && filledPollOptions >= 2)
        ) && postText.length <= charLimit

    fun attachMedia(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val current = selectedImageUris.toMutableList()
        var overflow = false
        for (uri in uris) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (current.any { it.toString() == uri.toString() }) continue
            if (current.size >= maxMedia) {
                overflow = true
                continue
            }
            current.add(uri)
        }
        selectedImageUris = current
        if (overflow) {
            Toast.makeText(context, "You can attach up to $maxMedia items", Toast.LENGTH_SHORT).show()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = maxMedia)
    ) { uris -> attachMedia(uris) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris -> attachMedia(uris) }

    val gifFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) attachMedia(listOf(uri)) }

    val gradientBrush = if (isDark) {
        Brush.horizontalGradient(colors = listOf(Color(0xFFA855F7), Color(0xFF14B8A6)))
    } else {
        Brush.horizontalGradient(colors = listOf(Color(0xFF0B57D0), Color(0xFF0284C7)))
    }
    val accentColor = if (isDark) Color(0xFF14B8A6) else MaterialTheme.colorScheme.primary
    var isContestSelected by remember { mutableStateOf(false) }
    var showContestDialog by remember { mutableStateOf(false) }
    var contestHashtagInput by remember { mutableStateOf("") }
    var managerAccessCodeInput by remember { mutableStateOf("") }
    val contestViewModel: ContestViewModel = hiltViewModel()
    val activeContestTags by contestViewModel.activeHashtags.collectAsState()
    val postScope = rememberCoroutineScope()
    LaunchedEffect(Unit) { contestViewModel.loadActiveHashtags() }
    var artistTags by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val remaining = charLimit - postText.length
                        Text(
                            text = "$remaining",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remaining < 0) Color(0xFFEF4444) else accentColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (!canPost || isPostingState) return@Button
                            if (recording) {
                                runCatching { recorder?.stop(); recorder?.release() }
                                recorder = null
                                recording = false
                            }
                            if (showPoll && filledPollOptions < 2) {
                                Toast.makeText(context, "Add at least two poll choices", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isPostingState = true
                            postScope.launch {
                            val contestTag = contestHashtagInput.trim().removePrefix("#").lowercase()
                            if (isContestSelected) {
                                if (contestTag.isBlank() || managerAccessCodeInput.isBlank()) {
                                    isPostingState = false
                                    Toast.makeText(context, "Enter the contest hashtag and manager code", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                val redeemed = contestViewModel.redeem(contestTag, managerAccessCodeInput)
                                if (redeemed.isFailure) {
                                    isPostingState = false
                                    Toast.makeText(context, redeemed.exceptionOrNull()?.message ?: "Invalid manager code", Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                            }

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
                                tags = buildList {
                                    if (isContestSelected && contestTag.isNotBlank()) add(contestTag)
                                    if (!selectedCategory.equals("All", true) && selectedCategory.isNotBlank()) {
                                        add(selectedCategory.lowercase())
                                    }
                                },
                                pollOptions = finalPoll,
                                location = null,
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
                                ),
                                onComplete = { ok ->
                                    isPostingState = false
                                    if (ok) onPostCreated()
                                }
                            )
                            }
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
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                            tint = accentColor,
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
                            color = accentColor
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Contest Checkbox & Host Option Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { isContestSelected = !isContestSelected }
                        ) {
                            Checkbox(
                                checked = isContestSelected,
                                onCheckedChange = { isContestSelected = it },
                                colors = CheckboxDefaults.colors(checkedColor = accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🏆 Enter Contest",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = {
                                com.tpsk.tj.common.util.GlobalNavigation.navigateTo(Screen.CreateContest.route)
                            }
                        ) {
                            Text(
                                text = "+ Host Contest",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    if (isContestSelected) {
                        val currentCleanTag = contestHashtagInput.trim().removePrefix("#").lowercase()
                        val isValidTag = currentCleanTag.isBlank() || activeContestTags.isEmpty() || activeContestTags.any {
                            it.removePrefix("#").equals(currentCleanTag, ignoreCase = true)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = contestHashtagInput,
                                onValueChange = { contestHashtagInput = it },
                                label = { Text("Contest Hashtag (e.g. #MissThembisa)") },
                                isError = !isValidTag,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!isValidTag) {
                                Text(
                                    text = "Hashtag must match an active contest (e.g. #MissThembisa)",
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp
                                )
                            }
                            OutlinedTextField(
                                value = managerAccessCodeInput,
                                onValueChange = { managerAccessCodeInput = it },
                                label = { Text("Manager Access Code (e.g. MGR-THEMB-1001)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Tool Icons Row
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
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                        ) {
                            Icon(Icons.Outlined.Image, contentDescription = "Photos and videos", tint = accentColor)
                        }

                        IconButton(
                            onClick = {
                                videoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Outlined.Videocam, contentDescription = "Video", tint = accentColor)
                        }

                        IconButton(
                            onClick = { showPoll = !showPoll }
                        ) {
                            Icon(Icons.Outlined.Poll, contentDescription = "Poll", tint = if (showPoll) Color(0xFF8A2BE2) else accentColor)
                        }

                        IconButton(onClick = {
                            gifQuery = ""
                            gifResults = emptyList()
                            gifError = null
                            showGifPicker = true
                            mediaViewModel.searchGifs("trending") { gifs, error ->
                                gifResults = gifs
                                gifError = error
                            }
                        }) {
                            Text("GIF", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            Icon(if (recording) Icons.Default.Stop else Icons.Outlined.Mic, contentDescription = "Voice note", tint = accentColor)
                        }

                        IconButton(onClick = {
                            val image = selectedImageUris.firstOrNull { uri ->
                                val mime = context.contentResolver.getType(uri).orEmpty()
                                mime.isBlank() || mime.startsWith("image/")
                            }?.toString()
                            if (image == null) {
                                Toast.makeText(context, "Add a picture first", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            Toast.makeText(context, "Writing a caption…", Toast.LENGTH_SHORT).show()
                            mediaViewModel.artistCaption(image) { caption ->
                                val failed = caption.isBlank() ||
                                    caption.startsWith("AI caption") ||
                                    caption.startsWith("Could not upload")
                                if (failed) {
                                    Toast.makeText(context, caption.ifBlank { "AI caption is unavailable" }, Toast.LENGTH_LONG).show()
                                } else {
                                    postText = caption.take(charLimit)
                                }
                            }
                        }) {
                            Icon(Icons.Default.Star, "AI caption", tint = Color(0xFF8A2BE2))
                        }
                        IconButton(onClick = { showAiAssistSheet = true }) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Assist",
                                tint = accentColor
                            )
                        }

                        IconButton(onClick = { showSchedule = true }) {
                            Icon(Icons.Outlined.Schedule, contentDescription = "Schedule", tint = accentColor)
                        }

                        IconButton(onClick = { showCollaborators = true }) {
                            Icon(Icons.Outlined.Group, contentDescription = "Collaborators", tint = accentColor)
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
                                color = accentColor,
                                startAngle = -90f,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                            )
                        }
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { innerTextField ->
                            if (postText.isEmpty()) {
                                Text(
                                    text = if (replyToPost != null) "Post your reply..." else "Share your thoughts...",
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Add a mood pill button (28.dp compact height)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .height(28.dp)
                            .clickable { showMood = true }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (selectedMood.isBlank()) "Add a mood" else selectedMood,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = accentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CATEGORY Section
            Text(
                text = "CATEGORY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val categoriesList = listOf(
                "All", "Technology", "Music", "Gaming",
                "Sports", "News", "Entertainment", "Art",
                "Lifestyle", "Business", "Education", "Health",
                "Food", "Comedy", "Relationships"
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                categoriesList.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    AppFilterChip(
                        text = cat,
                        isSelected = isSelected,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            // TEMPLATES Section
            Text(
                text = "TEMPLATES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            val templates = listOf(
                "🔥 Hot take" to "Hot take: ",
                "❓ Question" to "Question for the community: ",
                "🧵 Thread" to "Thread 🧵 (1/N):\n"
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                templates.forEach { (label, prefix) ->
                    AppFilterChip(
                        text = label,
                        isSelected = false,
                        onClick = {
                            if (!postText.startsWith(prefix)) {
                                postText = prefix + postText
                            }
                        }
                    )
                }
            }

            if (currentUser?.isArtist == true) {
                Text(
                    text = "ARTIST",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Add to Portfolio", "Process / WIP", "Part of Series", "Open for Commissions")) { tag ->
                        val selected = artistTags.contains(tag)
                        AppFilterChip(
                            text = tag,
                            isSelected = selected,
                            onClick = {
                                artistTags = if (selected) artistTags - tag else artistTags + tag
                            }
                        )
                    }
                }
            }

            if (showPoll) {
                Text(
                    text = "POLL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                pollOptions.forEachIndexed { index, option ->
                    OutlinedTextField(
                        value = option,
                        onValueChange = { value ->
                            pollOptions = pollOptions.toMutableList().also { it[index] = value }
                        },
                        label = { Text("Choice ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }
                if (pollOptions.size < 4) {
                    TextButton(onClick = { pollOptions = pollOptions + "" }) { Text("Add choice") }
                }
            }

            if (voicePath != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (recording) Icons.Default.Stop else Icons.Outlined.Mic, contentDescription = null, tint = accentColor)
                    Text(
                        text = if (recording) "Recording voice note" else "Voice note attached",
                        modifier = Modifier.padding(start = 8.dp).weight(1f),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = {
                        runCatching { recorder?.stop(); recorder?.release() }
                        recorder = null
                        recording = false
                        voicePath = null
                    }) { Text("Remove") }
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
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            val mime = context.contentResolver.getType(uri).orEmpty()
                            val isVideo = mime.startsWith("video/") || com.tpsk.tj.common.util.MediaUtils.isVideo(uri.toString())
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isVideo) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center).size(36.dp)
                                )
                            }
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
    if (showGifPicker) {
        AlertDialog(
            onDismissRequest = { showGifPicker = false },
            title = { Text("GIF") },
            text = {
                Column {
                    OutlinedTextField(
                        value = gifQuery,
                        onValueChange = {
                            gifQuery = it
                            mediaViewModel.searchGifs(it.ifBlank { "trending" }) { gifs, error ->
                                gifResults = gifs
                                gifError = error
                            }
                        },
                        label = { Text("Search GIFs") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!gifError.isNullOrBlank()) {
                        Text(gifError!!, color = Color(0xFFEF4444), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        items(gifResults) { gif ->
                            AsyncImage(
                                model = gif.preview.ifBlank { gif.url },
                                contentDescription = gif.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (gif.url.isNotBlank()) {
                                            attachMedia(listOf(Uri.parse(gif.url)))
                                            showGifPicker = false
                                        }
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showGifPicker = false
                    gifFilePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.SingleMimeType("image/gif")))
                }) { Text("Gallery") }
            },
            dismissButton = { TextButton(onClick = { showGifPicker = false }) { Text("Close") } }
        )
    }
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

    if (showContestDialog) {
        AlertDialog(
            onDismissRequest = { showContestDialog = false },
            title = { Text("🏆 Enter Contest", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = contestHashtagInput,
                        onValueChange = { contestHashtagInput = it },
                        label = { Text("Contest Hashtag (e.g. #MissThembisa)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = managerAccessCodeInput,
                        onValueChange = { managerAccessCodeInput = it },
                        label = { Text("Verified Manager Access Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contestHashtagInput.isNotBlank()) {
                            val tag = if (contestHashtagInput.startsWith("#")) contestHashtagInput else "#$contestHashtagInput"
                            if (!postText.contains(tag)) {
                                postText = if (postText.isBlank()) tag else "$postText $tag"
                            }
                        }
                        showContestDialog = false
                    }
                ) {
                    Text("Attach Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

}
