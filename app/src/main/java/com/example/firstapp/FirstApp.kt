package com.example.firstapp

import android.app.Application
import com.example.firstapp.data.TokenManager

class FirstApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TokenManager.initialize(this)
        AppContainer.initialize(this)
    }
}
