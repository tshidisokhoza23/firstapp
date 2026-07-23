package com.example.firstapp.settings.ui.viewmodel

import com.example.firstapp.BuildConfig
import com.example.firstapp.common.data.network.NetworkEnvironmentManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class HelpSettingsViewModel @Inject constructor(
    val envManager: NetworkEnvironmentManager
) : ViewModel() {
    private val _appVersion = MutableStateFlow("v${BuildConfig.VERSION_NAME}")
    val appVersion: StateFlow<String> = _appVersion

    fun contactSupport(query: String) {
        // Logic to send query to support
    }
}
