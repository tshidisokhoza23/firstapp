package com.example.firstapp.auth.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.firstapp.auth.ui.screens.LoginScreen
import com.example.firstapp.auth.ui.screens.OtpVerificationScreen
import com.example.firstapp.auth.ui.viewmodel.AuthViewModel
import com.example.firstapp.ui.navigation.Screen
import java.util.Collections

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    composable(Screen.Login.route) {
        val authViewModel: AuthViewModel = hiltViewModel()
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            },
            onNavigateToOtp = { phone, user, email, name, pass ->
                navController.navigate(Screen.OtpVerification.createRoute(phone, user, email, name, pass))
            },
            viewModel = authViewModel
        )
    }

    composable(
        route = Screen.OtpVerification.route,
        arguments = listOf(
            navArgument("phoneNumber") { type = NavType.StringType },
            navArgument("username") { type = NavType.StringType },
            navArgument("email") { type = NavType.StringType },
            navArgument("displayName") { type = NavType.StringType },
            navArgument("password") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
        val usernameRaw = backStackEntry.arguments?.getString("username") ?: "NONE"
        val emailRaw = backStackEntry.arguments?.getString("email") ?: "NONE"
        val displayNameRaw = backStackEntry.arguments?.getString("displayName") ?: "NONE"
        val passwordRaw = backStackEntry.arguments?.getString("password") ?: "NONE"
        
        val username = if (usernameRaw == "NONE") null else usernameRaw
        val email = if (emailRaw == "NONE") null else emailRaw
        val displayName = if (displayNameRaw == "NONE") null else displayNameRaw
        val password = if (passwordRaw == "NONE") null else passwordRaw

        OtpVerificationScreen(
            phoneNumber = phoneNumber,
            username = username,
            email = email,
            displayName = displayName,
            password = password,
            onVerificationSuccess = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            },
            onBack = { navController.popBackStack() }
        )
    }
}
