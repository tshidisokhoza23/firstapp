package com.example.firstapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.*
import com.example.firstapp.data.TokenManager
import com.example.firstapp.ui.screens.LoginScreen
import com.example.firstapp.ui.screens.MainScreen
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.FirstAppTheme
import com.google.android.gms.ads.MobileAds
import com.google.android.libraries.places.api.Places

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenManager.initialize(applicationContext)

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this) {}
        
        // Initialize Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyAcwjsoRCNMFsX5ayuuYC1CoNqj8NVP3Kw")
        }
        
        // Ensure system bars match our AppBackground
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AppBackground.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(AppBackground.toArgb())
        )

        setContent {
            FirstAppTheme {
                var isLoggedIn by remember { mutableStateOf(TokenManager.isLoggedIn()) }

                if (isLoggedIn) {
                    MainScreen(onLogout = {
                        TokenManager.clearToken()
                        isLoggedIn = false
                    })
                } else {
                    LoginScreen(onLoginSuccess = { isLoggedIn = true })
                }
            }
        }
    }
}
