package com.tpsk.tj.vocab.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.tpsk.tj.vocab.ui.screens.VocabScreen
import com.tpsk.tj.ui.navigation.Screen

fun NavGraphBuilder.vocabNavGraph(navController: NavHostController) {
    composable(Screen.Vocab.route) {
        VocabScreen(onBack = { navController.popBackStack() })
    }
}
