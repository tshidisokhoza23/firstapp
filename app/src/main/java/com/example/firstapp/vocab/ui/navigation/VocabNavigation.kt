package com.example.firstapp.vocab.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.firstapp.vocab.ui.screens.VocabScreen
import com.example.firstapp.ui.navigation.Screen

fun NavGraphBuilder.vocabNavGraph(navController: NavHostController) {
    composable(Screen.Vocab.route) {
        VocabScreen(onBack = { navController.popBackStack() })
    }
}
