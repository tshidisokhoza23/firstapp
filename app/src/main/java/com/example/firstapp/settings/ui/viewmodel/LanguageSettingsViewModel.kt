package com.example.firstapp.settings.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@HiltViewModel
class LanguageSettingsViewModel @Inject constructor() : ViewModel() {
    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    fun setSelectedLanguage(language: String) {
        _selectedLanguage.value = language
    }
}
