package com.example.firstapp.settings.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.settings.ui.viewmodel.LanguageSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit,
    viewModel: LanguageSettingsViewModel = hiltViewModel()
) {
    val languages = listOf(
        "English" to "Phone's language",
        "Afrikaans" to "Afrikaans",
        "isiZulu" to "Zulu",
        "isiXhosa" to "Xhosa",
        "Sepedi" to "Northern Sotho",
        "Setswana" to "Tswana",
        "Sesotho" to "Southern Sotho",
        "Xitsonga" to "Tsonga",
        "siSwati" to "Swati",
        "Tshivenda" to "Venda",
        "isiNdebele" to "Southern Ndebele"
    )
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App language", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground
                )
            )
        },
        bottomBar = {
            com.example.firstapp.common.ui.components.BannerAdView(
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            languages.forEach { (name, nativeName) ->
                LanguageItem(name, nativeName, name == selectedLanguage) {
                    viewModel.setSelectedLanguage(name)
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun LanguageItem(name: String, nativeName: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = AppAccent, unselectedColor = AppTextSecondary)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(nativeName, color = AppTextSecondary, fontSize = 13.sp)
        }
    }
}
