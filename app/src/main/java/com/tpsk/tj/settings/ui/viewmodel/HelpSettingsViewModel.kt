package com.tpsk.tj.settings.ui.viewmodel

import com.tpsk.tj.BuildConfig
import com.tpsk.tj.common.data.network.NetworkEnvironmentManager
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
