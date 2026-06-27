package com.example.firstapp.settings.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@HiltViewModel
class HelpSettingsViewModel @Inject constructor() : ViewModel() {
    private val _appVersion = MutableStateFlow("v1.0.6-unique")
    val appVersion: StateFlow<String> = _appVersion

    fun contactSupport(query: String) {
        // Logic to send query to support
    }
}
