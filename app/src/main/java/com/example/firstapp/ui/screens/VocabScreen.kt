package com.example.firstapp.ui.screens

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.AppContainer
import com.example.firstapp.vocab.ui.screens.home.HomeScreen
import com.example.firstapp.vocab.ui.screens.home.HomeViewModel
import com.example.firstapp.vocab.ui.screens.lesson.LessonScreen
import com.example.firstapp.vocab.ui.screens.lesson.LessonViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

@Composable
fun VocabScreen(
    onBack: () -> Unit
) {
    var currentLessonId by remember { mutableStateOf<String?>(null) }
    
    val userRepository = AppContainer.userRepository
    val lessonRepository = AppContainer.lessonRepository
    val adManager = AppContainer.adManager

    val homeViewModel: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(userRepository, lessonRepository, adManager) as T
            }
        }
    )

    if (currentLessonId == null) {
        HomeScreen(
            onStartLesson = { currentLessonId = it },
            onUpgrade = { /* handle upgrade */ },
            viewModel = homeViewModel,
            onBack = onBack
        )
    } else {
        val lessonViewModel: LessonViewModel = viewModel(
            key = currentLessonId,
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LessonViewModel(lessonRepository, userRepository) as T
                }
            }
        )
        LessonScreen(
            lessonId = currentLessonId!!,
            onNavigateBack = { currentLessonId = null },
            viewModel = lessonViewModel
        )
    }
}
