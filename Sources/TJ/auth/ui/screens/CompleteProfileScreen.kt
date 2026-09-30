package com.tpsk.tj.auth.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.tpsk.tj.auth.ui.components.CountryPickerField
import com.tpsk.tj.common.util.Country
import com.tpsk.tj.common.util.CountryUtils
import com.tpsk.tj.auth.ui.viewmodel.AuthViewModel
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.model.DefaultCategories

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    onCompleteSuccess: () -> Unit,
    onNavigateToTerms: () -> Unit = {},
    viewModel: AuthViewModel
) {
    val context = LocalContext.current
    val currentUser = com.tpsk.tj.LocalCurrentUser.current
    val authState by viewModel.authState.collectAsState()

    var username by remember { 
        mutableStateOf(
            if (currentUser.username.contains("@")) "" else currentUser.username
        ) 
    }
    var phoneNumber by remember { mutableStateOf(currentUser.phoneNumber) }
    var selectedCountry by remember {
        mutableStateOf<Country>(
            CountryUtils.allCountries.find { it.dialCode.isNotBlank() && currentUser.phoneNumber.startsWith(it.dialCode) }
                ?: CountryUtils.getDefaultCountry(context)
        )
    }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(authState) {
        if (authState is Resource.Success) {
            onCompleteSuccess()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Complete Profile", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Just a few more details to get started!",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Forced visible Username
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("e.g. john_doe") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Forced visible Phone Number with Country Code selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountryPickerField(
                    selectedCountry = selectedCountry,
                    onCountrySelected = { selectedCountry = it },
                    modifier = Modifier.width(100.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { input ->
                        if (input.all { char -> char.isDigit() || char == '+' }) {
                            phoneNumber = input
                        }
                    },
                    label = { Text("Phone Number") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("e.g. 821234567 or 0821234567") },
                    supportingText = { Text("Supports 9/10 digits or country code", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "Select interests for your feed (3+ recommended)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DefaultCategories.filter { it.id != "all" }) { category ->
                    val isSelected = selectedCategories.contains(category.id)
                    Surface(
                        modifier = Modifier
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedCategories = if (isSelected) {
                                    selectedCategories - category.id
                                } else {
                                    selectedCategories + category.id
                                }
                            },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = category.name,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (authState is Resource.Error) {
                Text(
                    text = (authState as Resource.Error).message ?: "Error",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "By clicking Finish Setup, you agree to FirstApp's Terms of Service & EULA, and acknowledge our Privacy Policy.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clickable(onClick = onNavigateToTerms)
            )

            Button(
                onClick = {
                    val rawPhone = phoneNumber.trim()
                    val cleanPhone = if (rawPhone.isBlank()) null else CountryUtils.formatE164PhoneNumber(rawPhone, selectedCountry.dialCode)
                    viewModel.completeProfile(
                        displayName = null, // Handled later in settings
                        username = username.takeIf { it.isNotBlank() },
                        phoneNumber = cleanPhone,
                        categories = selectedCategories.toList().takeIf { it.isNotEmpty() }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = authState !is Resource.Loading && username.isNotBlank() && phoneNumber.isNotBlank()
            ) {
                if (authState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Finish Setup")
                }
            }
        }
    }
}
