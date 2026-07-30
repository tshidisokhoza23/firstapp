package com.example.firstapp.auth.ui.navigation

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.firstapp.auth.ui.screens.LoginScreen
import com.example.firstapp.auth.ui.screens.OtpVerificationScreen
import com.example.firstapp.auth.ui.viewmodel.AuthViewModel
import com.example.firstapp.ui.navigation.Screen

fun NavGraphBuilder.authNavGraph(navController: NavHostController) {
    composable(Screen.Login.route) {
        val authViewModel: AuthViewModel = hiltViewModel()
        LoginScreen(
            onLoginSuccess = {
                navController.navigate(Screen.Main.createRoute()) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateToOtp = { phone, user, email, name, pass ->
                navController.navigate(Screen.OtpVerification.createRoute(
                    phone,
                    user ?: "NONE",
                    email ?: "NONE",
                    name ?: "NONE",
                    pass ?: "NONE"
                )) {
                    launchSingleTop = true
                }
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
        // Use the ViewModel from the Login screen if it's still in the backstack
        val loginEntry = remember(backStackEntry) {
            try {
                navController.getBackStackEntry(Screen.Login.route)
            } catch (e: Exception) {
                null
            }
        }
        
        val authViewModel: AuthViewModel = if (loginEntry != null) {
            hiltViewModel(loginEntry)
        } else {
            hiltViewModel()
        }
        
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
                navController.navigate(Screen.Main.createRoute()) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onBack = { navController.popBackStack() },
            viewModel = authViewModel
        )
    }
}
