package com.tpsk.tj.vocab.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.runtime.*
import com.tpsk.tj.vocab.ui.screens.home.HomeScreen
import com.tpsk.tj.vocab.ui.screens.home.HomeViewModel
import com.tpsk.tj.vocab.ui.screens.lesson.LessonScreen
import com.tpsk.tj.vocab.ui.screens.lesson.LessonViewModel


@Composable
fun VocabScreen(
    onBack: () -> Unit
) {
    var currentLessonId by remember { mutableStateOf<String?>(null) }
    
    val homeViewModel: HomeViewModel = hiltViewModel()

    if (currentLessonId == null) {
        HomeScreen(
            onStartLesson = { currentLessonId = it },
            onUpgrade = { /* handle upgrade */ },
            viewModel = homeViewModel,
            onBack = onBack
        )
    } else {
        val lessonViewModel: LessonViewModel = hiltViewModel(key = currentLessonId)
        
        LaunchedEffect(currentLessonId) {
            lessonViewModel.loadLesson(currentLessonId!!)
        }

        LessonScreen(
            lessonId = currentLessonId!!,
            onNavigateBack = { currentLessonId = null },
            viewModel = lessonViewModel
        )
    }
}
