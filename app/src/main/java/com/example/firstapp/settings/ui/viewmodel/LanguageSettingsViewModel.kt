package com.example.firstapp.settings.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LanguageSettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {
    val selectedLanguage: StateFlow<String> = repository.selectedLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "English")

    fun setSelectedLanguage(language: String) {
        viewModelScope.launch { repository.setSelectedLanguage(language) }
    }
}
