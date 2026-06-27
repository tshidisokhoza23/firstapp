package com.example.firstapp.vocab.ui.screens.lesson

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.vocab.model.ContentType
import com.example.firstapp.vocab.model.LessonContent
import com.example.firstapp.vocab.ui.components.VocabAdBlock
import com.example.firstapp.vocab.ui.util.UiTranslations
import com.example.firstapp.vocab.ui.util.rememberLessonFeedbackPlayer
import com.example.firstapp.vocab.ui.util.rememberLessonSpeechPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    viewModel: LessonViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val feedbackPlayer = rememberLessonFeedbackPlayer()

    LaunchedEffect(uiState.showFeedback) {
        if (uiState.showFeedback) {
            if (uiState.isCorrect) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                feedbackPlayer.playCorrect()
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                feedbackPlayer.playError()
            }
        }
    }
    val nativeLang = uiState.user?.nativeLanguage ?: "English"
    val targetLang = uiState.lesson?.language ?: "English"
    val speechPlayer = rememberLessonSpeechPlayer(targetLang)

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        uiState.lesson?.titleTranslations?.get(nativeLang) 
                        ?: uiState.lesson?.titleTranslations?.get("English") 
                        ?: "Loading..."
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.isFinished) {
            val maxScore = (uiState.lesson?.content?.count { it.type == ContentType.QUESTION || it.type == ContentType.SPEAKING } ?: 0) * 10
            LessonFinishedScreen(
                score = uiState.score,
                maxScore = maxScore,
                missedPrompts = uiState.missedPrompts,
                nativeLang = nativeLang,
                onFinish = onNavigateBack,
                onRetry = { viewModel.retryLesson() }
            )
        } else {
            val lesson = uiState.lesson ?: return@Scaffold
            val currentContent = lesson.content.getOrNull(uiState.currentStep) ?: return@Scaffold

            val progress = (uiState.currentStep + 1).toFloat() / lesson.content.size
            val currentStepText = "${uiState.currentStep + 1}/${lesson.content.size}"
            
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.weight(1f).height(10.dp).clip(CircleShape),
                        color = com.example.firstapp.common.ui.theme.BoyishPurple,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = currentStepText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    when (currentContent.type) {
                        ContentType.TEACH -> {
                            TeachingCard(
                                text = currentContent.translations["English"] ?: "",
                                targetText = currentContent.translations[targetLang] ?: "",
                                explanation = currentContent.explanation,
                                audioText = currentContent.audioText,
                                phonetic = currentContent.phonetic,
                                partOfSpeech = currentContent.partOfSpeech,
                                nativeLang = nativeLang,
                                onNext = { viewModel.onNextClicked() },
                                onBack = { viewModel.onBackClicked() },
                                canGoBack = viewModel.canGoBack(),
                                canListen = speechPlayer.isReady,
                                onListen = { speechPlayer.speak(currentContent.audioText) }
                            )
                        }
                        ContentType.EXAMPLE -> {
                            ExampleScreen(
                                englishSentence = currentContent.translations["English"] ?: "",
                                targetSentence = currentContent.translations[targetLang] ?: "",
                                nativeLang = nativeLang,
                                onNext = { viewModel.onNextClicked() },
                                onBack = { viewModel.onBackClicked() },
                                canGoBack = viewModel.canGoBack(),
                                onListen = { speechPlayer.speak(currentContent.audioText) }
                            )
                        }
                        ContentType.QUESTION -> {
                            QuestionContent(
                                content = currentContent,
                                uiState = uiState,
                                nativeLang = nativeLang,
                                onAnswerSelected = viewModel::onAnswerSelected,
                                onNext = viewModel::onNextClicked,
                                onBack = viewModel::onBackClicked,
                                canGoBack = viewModel.canGoBack(),
                                canGoNext = viewModel.canGoNext(),
                                canListen = speechPlayer.isReady,
                                onListenOption = { option -> speechPlayer.speak(option) }
                            )
                        }
                        ContentType.SPEAKING -> {
                            SpeakingContent(
                                content = currentContent,
                                uiState = uiState,
                                nativeLang = nativeLang,
                                onNext = viewModel::onNextClicked,
                                onBack = viewModel::onBackClicked,
                                canGoBack = viewModel.canGoBack(),
                                onListen = { speechPlayer.speak(currentContent.audioText) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeachingCard(
    text: String,
    targetText: String,
    explanation: String,
    audioText: String,
    phonetic: String,
    partOfSpeech: String,
    nativeLang: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
    canGoBack: Boolean,
    canListen: Boolean,
    onListen: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (partOfSpeech.isNotBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = UiTranslations.getString(partOfSpeech, nativeLang).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = targetText,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (phonetic.isNotBlank()) {
                        Text(
                            text = phonetic,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        if (audioText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = audioText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onListen,
                enabled = canListen
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = UiTranslations.getString("listen", nativeLang))
                Spacer(modifier = Modifier.width(8.dp))
                Text(UiTranslations.getString("listen", nativeLang))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = canGoBack,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(UiTranslations.getString("back", nativeLang), style = MaterialTheme.typography.titleMedium)
            }
            
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(UiTranslations.getString("next", nativeLang), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun ExampleScreen(
    englishSentence: String,
    targetSentence: String,
    nativeLang: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
    canGoBack: Boolean,
    onListen: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Context Example",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = englishSentence,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = targetSentence,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        FilledTonalButton(onClick = onListen) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Listen to Sentence")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = canGoBack,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(UiTranslations.getString("back", nativeLang), style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(UiTranslations.getString("next", nativeLang), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}


@Composable
fun QuestionContent(
    content: LessonContent,
    uiState: LessonUiState,
    nativeLang: String,
    onAnswerSelected: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    canGoBack: Boolean,
    canGoNext: Boolean,
    canListen: Boolean,
    onListenOption: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = content.translations[nativeLang] ?: content.translations["English"] ?: "",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            content.options.forEachIndexed { index, option ->
                val isSelected = uiState.selectedAnswerIndex == index
                val isCorrect = uiState.showFeedback && content.correctAnswerIndex == index
                val isWrong = uiState.showFeedback && isSelected && !uiState.isCorrect

                val color = when {
                    isCorrect -> Color(0xFF4CAF50)
                    isWrong -> colorScheme.error
                    isSelected -> colorScheme.primary
                    else -> colorScheme.surfaceVariant
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (!uiState.showFeedback) onAnswerSelected(index) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isSelected || isCorrect || isWrong) color else colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, color)
                    ) {
                        Text(text = option, modifier = Modifier.padding(8.dp))
                    }

                    FilledTonalIconButton(
                        onClick = { onListenOption(option) },
                        enabled = canListen
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = UiTranslations.getString("listen_option", nativeLang)
                        )
                    }
                }
            }

            if (!uiState.showFeedback) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = UiTranslations.getString("listen_hint", nativeLang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose an answer, then continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (uiState.showFeedback) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (uiState.isCorrect) 
                    Color(0xFFE8F5E9).copy(alpha = 0.8f) 
                else 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                border = BorderStroke(
                    1.5.dp,
                    if (uiState.isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (uiState.isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (uiState.isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isCorrect)
                                UiTranslations.getString("correct", nativeLang)
                            else
                                UiTranslations.getString("incorrect", nativeLang),
                            color = if (uiState.isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (content.explanation.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = content.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = canGoBack,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(UiTranslations.getString("back", nativeLang), style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = onNext,
                enabled = canGoNext,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.firstapp.common.ui.theme.BoyishPurple)
            ) {
                Text(
                    if (uiState.currentStep < (uiState.lesson?.content?.size ?: 0) - 1)
                        UiTranslations.getString("next", nativeLang)
                    else
                        UiTranslations.getString("finish", nativeLang),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}


@Composable
fun SpeakingContent(
    content: LessonContent,
    uiState: LessonUiState,
    nativeLang: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
    canGoBack: Boolean,
    onListen: () -> Unit
) {
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Speak this phrase",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = content.translations[nativeLang] ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = content.translations[uiState.lesson?.language ?: "English"] ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (recognizedText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = "You said:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = recognizedText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (recognizedText.lowercase() == content.audioText.lowercase()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isListening) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    if (!isListening) {
                        isListening = true
                        scope.launch {
                            delay(2000) // Simulate listening
                            isListening = false
                            recognizedText = content.audioText // Simulate correct recognition
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                contentDescription = "Microphone",
                modifier = Modifier.size(40.dp),
                tint = if (isListening) Color.Red else MaterialTheme.colorScheme.primary
            )
            
            if (isListening) {
                val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "mic_scale"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .border(2.dp, Color.Red.copy(alpha = 0.5f), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                enabled = canGoBack,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(UiTranslations.getString("back", nativeLang), style = MaterialTheme.typography.titleMedium)
            }

            Button(
                onClick = onNext,
                enabled = recognizedText.isNotEmpty() || !isListening,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.firstapp.common.ui.theme.BoyishPurple)
            ) {
                Text(UiTranslations.getString("next", nativeLang), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}


@Composable
fun LessonFinishedScreen(
    score: Int,
    maxScore: Int,
    missedPrompts: List<String>,
    nativeLang: String,
    onFinish: () -> Unit,
    onRetry: () -> Unit
) {
    val isPerfect = maxScore > 0 && score == maxScore

    Box(modifier = Modifier.fillMaxSize()) {
        if (isPerfect) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        speed = 0f,
                        maxSpeed = 30f,
                        damping = 0.9f,
                        spread = 360,
                        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb980ff),
                        position = Position.Relative(0.5, 0.3),
                        emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = UiTranslations.getString("lesson_completed", nativeLang), 
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isPerfect) {
                UiTranslations.getString("earned_xp", nativeLang).format(score)
            } else {
                UiTranslations.getString("pass_perfect_required", nativeLang).format(score, maxScore)
            }, 
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )

        if (!isPerfect && missedPrompts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppSecondary)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = UiTranslations.getString("review_missed_title", nativeLang),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    missedPrompts.take(4).forEach { prompt ->
                        Text(
                            text = "• $prompt",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(UiTranslations.getString("retry", nativeLang), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = com.example.firstapp.common.ui.theme.BoyishPurple)
        ) {
            Text(UiTranslations.getString("back_home", nativeLang), style = MaterialTheme.typography.titleMedium)
        }
    }
}
}
