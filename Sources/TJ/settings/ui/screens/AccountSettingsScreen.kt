package com.tpsk.tj.settings.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.common.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpsk.tj.settings.ui.viewmodel.AccountSettingsViewModel
import com.tpsk.tj.auth.ui.components.CountryPickerField
import com.tpsk.tj.common.util.CountryUtils
import com.tpsk.tj.common.util.Country
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(onBack: () -> Unit, viewModel: AccountSettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val toastMessage by viewModel.toastMessage.collectAsState()
    val securityNotificationsEnabled by viewModel.securityNotificationsEnabled.collectAsState()
    
    var showPhoneDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Account", 
                        color = MaterialTheme.colorScheme.onBackground, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back", 
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            com.tpsk.tj.common.ui.components.BannerAdView(
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsItem(
                icon = Icons.Default.Phone,
                title = "Change Phone Number",
                subtitle = "Tap to change your phone number",
                onClick = { showPhoneDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Two-step verification",
                subtitle = "Not available yet",
                onClick = {
                    Toast.makeText(context, "Two-step verification is not available yet", Toast.LENGTH_SHORT).show()
                }
            )
            SettingsItem(
                icon = Icons.Default.Email,
                title = "Change Email",
                subtitle = "Not available yet",
                onClick = {
                    Toast.makeText(context, "Email changes are not available yet", Toast.LENGTH_SHORT).show()
                }
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp), 
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Security notifications", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                    Text("Show notifications for security alerts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                Switch(
                    checked = securityNotificationsEnabled,
                    onCheckedChange = { 
                        viewModel.toggleSecurityNotifications(it)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showPhoneDialog) {
        ChangePhoneDialog(
            onDismiss = { showPhoneDialog = false },
            onConfirm = { newPhone ->
                viewModel.changePhoneNumber(newPhone)
                showPhoneDialog = false
            }
        )
    }
}

@Composable
fun ChangePhoneDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(CountryUtils.getDefaultCountry(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Change Phone Number", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column {
                Text("Enter your new phone number with country code.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    CountryPickerField(
                        selectedCountry = selectedCountry,
                        onCountrySelected = { selectedCountry = it },
                        modifier = Modifier.width(90.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input -> 
                            if (input.all { char: Char -> char.isDigit() }) {
                                phoneNumber = input 
                            }
                        },
                        label = { Text("New Number") },
                        placeholder = { Text("e.g. 821234567 or 0821234567") },
                        supportingText = { Text("Supports 9/10 digits or country code", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val currentPhone = phoneNumber
                if (currentPhone.isNotBlank()) {
                    onConfirm(CountryUtils.formatE164PhoneNumber(currentPhone, selectedCountry.dialCode))
                }
            }) {
                Text("Change", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                if (subtitle != null) {
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        }
    }
}
