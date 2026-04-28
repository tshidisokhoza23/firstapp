package com.example.firstapp.vocab.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.firstapp.MainActivity
import com.example.firstapp.vocab.model.Lesson
import com.example.firstapp.model.User
import com.example.firstapp.vocab.ui.util.UiTranslations
import com.example.firstapp.vocab.ui.components.VocabAdBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartLesson: (String) -> Unit,
    onUpgrade: () -> Unit,
    viewModel: HomeViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showLanguagePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val nativeLang = uiState.user.nativeLanguage

    val languages = listOf(
        "English" to "🇺🇸",
        "Spanish" to "🇪🇸",
        "French" to "🇫🇷",
        "German" to "🇩🇪",
        "Italian" to "🇮🇹",
        "Portuguese" to "🇵🇹",
        "Afrikaans" to "🇿🇦"
    )

    var previousLevel by remember { mutableIntStateOf(uiState.user.level) }
    var showLevelUpCelebration by remember { mutableStateOf(false) }
    var lessonToUnlock by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.user.level) {
        if (uiState.user.level > previousLevel && previousLevel != 0) {
            showLevelUpCelebration = true
        }
        previousLevel = uiState.user.level
    }

    if (showLevelUpCelebration) {
        LevelUpDialog(
            level = uiState.user.level,
            nativeLang = nativeLang,
            onDismiss = { showLevelUpCelebration = false }
        )
    }

    if (lessonToUnlock != null) {
        val adsRemaining = 2 - uiState.user.adsWatchedToday
        AlertDialog(
            onDismissRequest = { lessonToUnlock = null },
            title = { Text("Unlock Lesson") },
            text = { 
                Text(
                    if (adsRemaining > 0) 
                        "Watch $adsRemaining more short ad${if (adsRemaining > 1) "s" else ""} to unlock the next lesson!" 
                    else 
                        "You've watched enough ads! Click below to unlock."
                ) 
            },
            confirmButton = {
                Button(onClick = {
                    (context as? MainActivity)?.let { activity ->
                        viewModel.showAd(activity)
                    }
                    if (adsRemaining <= 1) {
                        lessonToUnlock = null
                    }
                }) {
                    Text(if (adsRemaining > 1) "Watch Ad" else "Unlock Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { lessonToUnlock = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MyVocab", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${uiState.user.nativeLanguage} → ${uiState.user.targetLanguage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showLanguagePicker = true }) {
                        Icon(Icons.Default.Language, contentDescription = "Select Languages")
                    }
                    if (!uiState.user.isPremium) {
                        Button(onClick = onUpgrade) {
                            Text(UiTranslations.getString("go_premium", nativeLang))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (showLanguagePicker) {
            ModalBottomSheet(
                onDismissRequest = { showLanguagePicker = false },
                sheetState = sheetState
            ) {
                var selectedTab by remember { mutableIntStateOf(0) }
                
                Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                    Text(
                        text = UiTranslations.getString("settings", nativeLang),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(UiTranslations.getString("i_speak", nativeLang)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(UiTranslations.getString("im_learning", nativeLang)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(languages) { (name, flag) ->
                            val isSelected = if (selectedTab == 0) 
                                uiState.user.nativeLanguage == name 
                            else 
                                uiState.user.targetLanguage == name

                            Surface(
                                onClick = {
                                    if (selectedTab == 0) {
                                        viewModel.onNativeLanguageSelected(name)
                                        selectedTab = 1
                                    } else {
                                        viewModel.onTargetLanguageSelected(name)
                                        showLanguagePicker = false
                                    }
                                },
                                shape = MaterialTheme.shapes.medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = flag, style = MaterialTheme.typography.headlineSmall)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                UserStatusCard(
                    user = uiState.user,
                    completedLessons = uiState.completedLessonsCount,
                    totalLessons = uiState.totalLessonsCount,
                    onBuyFreeze = viewModel::buyStreakFreeze
                )
            }

            item {
                DailyXpProgress(
                    currentXp = uiState.currentDailyXp,
                    goalXp = uiState.dailyXpGoal,
                    nativeLang = nativeLang
                )
            }

            item {
                DailyUnlockProgress(
                    adsWatched = uiState.user.adsWatchedToday,
                    nextLesson = uiState.lessons.getOrNull(uiState.unlockedCount),
                    nativeLang = nativeLang,
                    onWatchAd = {
                        (context as? MainActivity)?.let { activity ->
                            viewModel.showAd(activity)
                        }
                    }
                )
            }

            uiState.units.forEach { unit ->
                item {
                    UnitHeader(unit, nativeLang)
                }

                items(unit.lessons) { lesson ->
                    val isEnabled = true 
                    val isCompleted = (uiState.user.lessonMastery[lesson.id] ?: 0) >= 100

                    LessonItem(
                        title = lesson.titleTranslations[nativeLang] ?: lesson.titleTranslations["English"] ?: "",
                        language = lesson.language,
                        category = lesson.categoryTranslations[nativeLang] ?: lesson.categoryTranslations["English"] ?: "",
                        isEnabled = isEnabled,
                        isCompleted = isCompleted,
                        onClick = { onStartLesson(lesson.id) }
                    )
                }
            }
        }
    }
}
}

@Composable
fun DailyXpProgress(
    currentXp: Int,
    goalXp: Int,
    nativeLang: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = UiTranslations.getString("daily_goal", nativeLang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$currentXp / $goalXp XP",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { currentXp.toFloat() / goalXp },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                strokeCap = StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (currentXp >= goalXp) 
                    "Goal reached! You're crushing it! 🔥" 
                else 
                    "Keep going! ${goalXp - currentXp} more XP to hit your goal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LevelUpDialog(
    level: Int,
    nativeLang: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(UiTranslations.getString("continue", nativeLang))
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = UiTranslations.getString("level_up", nativeLang),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = UiTranslations.getString("level_reached", nativeLang).format(level),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
}

@Composable
fun UnitHeader(
    unit: LessonUnit,
    nativeLang: String
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = unit.title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "${UiTranslations.getString("mastery", nativeLang)}: ${(unit.progress * 100).toInt()}%",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { unit.progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            strokeCap = StrokeCap.Round,
            color = com.example.firstapp.ui.theme.BoyishPurple,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun UserStatusCard(
    user: User,
    completedLessons: Int,
    totalLessons: Int,
    onBuyFreeze: () -> Unit
) {
    val nativeLang = user.nativeLanguage
    val purple = com.example.firstapp.ui.theme.BoyishPurple
    val orange = com.example.firstapp.ui.theme.NiceOrange
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = com.example.firstapp.ui.theme.AppSecondary,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${UiTranslations.getString("level", nativeLang)} ${user.level}", 
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 20.sp
                )
                
                val xpInLevel = user.xp % 100
                val progress = xpInLevel / 100f
                val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = progress,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000),
                    label = "xpProgress"
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = purple,
                    trackColor = Color.White.copy(alpha = 0.05f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = UiTranslations.getString("lesson_progress", nativeLang)
                        .format(completedLessons, totalLessons),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = orange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${user.streak}", 
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    onClick = onBuyFreeze,
                    shape = RoundedCornerShape(12.dp),
                    color = (if (user.streakFreezes > 0) orange else Color.Gray).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, (if (user.streakFreezes > 0) orange else Color.Gray).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = "Streak Freeze",
                            tint = if (user.streakFreezes > 0) orange else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${user.streakFreezes}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (user.streakFreezes > 0) orange else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyUnlockProgress(
    adsWatched: Int,
    nextLesson: Lesson?,
    nativeLang: String,
    onWatchAd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val lessonTitle = nextLesson?.titleTranslations?.get(nativeLang)
                ?: nextLesson?.titleTranslations?.get("English")
                ?: "..."
                
            Text(
                text = UiTranslations.getString("unlock_daily", nativeLang).format(lessonTitle),
                fontWeight = FontWeight.Bold
            )
            Text(
                UiTranslations.getString("unlock_desc", nativeLang),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { adsWatched / 2f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = com.example.firstapp.ui.theme.NiceOrange,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onWatchAd,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.firstapp.ui.theme.NiceOrange)
            ) {
                val buttonText = "${UiTranslations.getString("watch_ad", nativeLang)} ($adsWatched/2)"
                Text(buttonText)
            }
        }
    }
}

@Composable
fun LessonItem(
    title: String,
    language: String,
    category: String,
    isEnabled: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = language,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • $category",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isEnabled) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = MaterialTheme.colorScheme.outline
                )
            } else if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF4CAF50)
                )
            }
        }
    }
}
