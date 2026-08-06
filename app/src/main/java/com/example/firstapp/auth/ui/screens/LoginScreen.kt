@file:Suppress("DEPRECATION")

package com.example.firstapp.auth.ui.screens

import kotlin.text.Regex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.text.input.VisualTransformation

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.example.firstapp.R
import com.example.firstapp.common.ui.theme.LogoBlueDark
import com.example.firstapp.common.ui.theme.LogoBlueLight
import android.widget.Toast

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.firstapp.common.data.api.ForgotPasswordRequest
import java.net.ConnectException

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.auth.ui.viewmodel.AuthViewModel
import com.example.firstapp.auth.ui.components.CountryPickerField
import com.example.firstapp.auth.util.SmsBroadcastReceiver
import com.example.firstapp.common.util.Resource
import com.example.firstapp.common.util.CountryUtils
import com.example.firstapp.common.util.Country
import com.example.firstapp.common.util.AppSignatureHelper
import com.google.android.gms.auth.api.phone.SmsRetriever
import android.content.IntentFilter
import androidx.core.content.ContextCompat

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToOtp: (String, String?, String?, String?, String?) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotPasswordId by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val otpState by viewModel.otpState.collectAsState()
    val fieldErrors by viewModel.fieldErrors.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        selectedCountry = CountryUtils.getDefaultCountry(context)
    }

    LaunchedEffect(authState) {
        if (authState is Resource.Success) {
            onLoginSuccess()
        }
    }

    val errorMessage = (authState as? Resource.Error)?.message ?: (otpState as? Resource.Error)?.message
    val isLoading = authState is Resource.Loading || otpState is Resource.Loading

    // Show toast on error
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(otpState) {
        if (otpState is Resource.Success) {
            val phone = viewModel.lastRequestedPhone
            if (isSignUp) {
                onNavigateToOtp(phone, username.trim(), email.trim(), displayName.trim(), password)
            } else {
                onNavigateToOtp(phone, null, null, null, null)
            }
            viewModel.resetOtpState()
        }
    }

    fun performLogin() {
        viewModel.clearFieldErrors()
        val cleanUsername = username.trim().lowercase()
        val cleanEmail = email.trim().lowercase()
        val cleanDisplayName = displayName.trim()
        val rawPhone = phoneNumber.trim()
        val cleanPassword = password

        var hasError = false
        if (cleanUsername.isBlank()) {
            viewModel.setFieldError("username", "Username is required")
            hasError = true
        }
        
        if (cleanPassword.isBlank()) {
            viewModel.setFieldError("password", "Password is required")
            hasError = true
        }

        if (isSignUp) {
            if (cleanUsername.isBlank()) {
                viewModel.setFieldError("username", "Username is required")
                hasError = true
            } else if (cleanUsername.length < 3) {
                viewModel.setFieldError("username", "Username must be at least 3 characters")
                hasError = true
            } else if (!cleanUsername.matches(Regex("^[a-z0-9._]*$"))) {
                viewModel.setFieldError("username", "Username can only contain lowercase letters, numbers, dots and underscores")
                hasError = true
            }

            if (cleanPassword.isBlank()) {
                viewModel.setFieldError("password", "Password is required")
                hasError = true
            } else if (cleanPassword.length < 8) {
                viewModel.setFieldError("password", "Password must be at least 8 characters")
                hasError = true
            }

            if (cleanDisplayName.isBlank()) {
                viewModel.setFieldError("displayName", "Display name is required")
                hasError = true
            } else if (cleanDisplayName.length < 2) {
                viewModel.setFieldError("displayName", "Display name is too short")
                hasError = true
            }

            if (cleanEmail.isBlank()) {
                viewModel.setFieldError("email", "Email is required")
                hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                viewModel.setFieldError("email", "Enter a valid email address")
                hasError = true
            }

            if (rawPhone.isBlank()) {
                viewModel.setFieldError("phone", "Phone number is required")
                hasError = true
            }

            val cleanPhone = if (rawPhone.startsWith("+")) rawPhone else {
                val dialCode = selectedCountry?.dialCode ?: ""
                val formatted = if (rawPhone.startsWith("0")) rawPhone.substring(1) else rawPhone
                dialCode + formatted
            }

            if (rawPhone.isNotBlank() && cleanPhone.length < 10) {
                viewModel.setFieldError("phone", "Enter a valid phone number")
                hasError = true
            }

            if (password != confirmPassword) {
                viewModel.setFieldError("confirmPassword", "Passwords do not match")
                hasError = true
            }

            if (hasError) return

            // Check availability with backend before requesting OTP
            viewModel.checkAvailability(cleanUsername, cleanEmail, cleanPhone) { isAvailable ->
                if (isAvailable) {
                    val client = SmsRetriever.getClient(context)
                    client.startSmsRetriever()
                    viewModel.requestOTP(cleanPhone)
                }
            }
        } else {
            if (!hasError) {
                val client = SmsRetriever.getClient(context)
                client.startSmsRetriever()
                viewModel.login(cleanUsername, cleanPassword)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        
        // Logo Container
        Surface(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(24.dp),
            color = if (isSystemInDarkTheme()) Color(0xFF1E1E1E) else Color(0xFFF5F5F7),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(70.dp),
                    tint = Color.Unspecified
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (isSignUp) "Join FirstApp today" else "Login to continue",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Username Field
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it
                    viewModel.setFieldError("username", null)
                    viewModel.resetState()
                },
                label = { Text("Username") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isError = fieldErrors.containsKey("username"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LogoBlueDark,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedLabelColor = LogoBlueDark,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                    focusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                )
            )
            ValidationText(fieldErrors["username"])
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSignUp) {
            // Email Field
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.setFieldError("email", null) },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    isError = fieldErrors.containsKey("email"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LogoBlueDark,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        unfocusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                        focusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                    )
                )
                ValidationText(fieldErrors["email"])
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Display Name
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; viewModel.setFieldError("displayName", null) },
                    label = { Text("Display Name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    isError = fieldErrors.containsKey("displayName"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LogoBlueDark,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        unfocusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                        focusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                    )
                )
                ValidationText(fieldErrors["displayName"])
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Phone Number
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val country = selectedCountry
                    if (country != null) {
                        CountryPickerField(
                            selectedCountry = country,
                            onCountrySelected = { selectedCountry = it },
                            modifier = Modifier.width(100.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            if (input.all { char: Char -> char.isDigit() || char == '+' }) {
                                phoneNumber = input
                            }
                            viewModel.setFieldError("phone", null)
                        },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g. 812345678") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        isError = fieldErrors.containsKey("phone"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LogoBlueDark,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            unfocusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                            focusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                        )
                    )
                }
                ValidationText(fieldErrors["phone"])
            }
            Spacer(modifier = Modifier.height(16.dp))
        }


        // Password Field
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    viewModel.setFieldError("password", null)
                    viewModel.resetState()
                },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                isError = fieldErrors.containsKey("password"),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LogoBlueDark,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedLabelColor = LogoBlueDark,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                    focusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                )
            )
            ValidationText(fieldErrors["password"])
        }

        if (isSignUp) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; viewModel.setFieldError("confirmPassword", null) },
                    label = { Text("Confirm Password") },
                    isError = fieldErrors.containsKey("confirmPassword"),
                    visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                            Icon(
                                imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LogoBlueDark,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        unfocusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7),
                        focusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
                    )
                )
                ValidationText(fieldErrors["confirmPassword"])
            }
        }

        if (!isSignUp) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = { showForgotPassword = true }) {
                    Text(
                        "Forgot Password?", 
                        color = LogoBlueDark, 
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Gradient Login Button
        val gradientBrush = Brush.linearGradient(
            colors = listOf(LogoBlueDark, LogoBlueLight)
        )

        Button(
            onClick = { performLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(gradientBrush, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // Use background gradient
                contentColor = Color.White
            ),
            enabled = !isLoading,
            contentPadding = PaddingValues() // Important to let gradient fill
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp), 
                        color = Color.White, 
                        strokeWidth = 2.dp
                    )
                } else {
                    val buttonText = if (!isSignUp) "Login" else "Sign Up"
                    Text(buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isSignUp) "Already have an account? " else "Don't have an account? ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
            Text(
                text = if (isSignUp) "Login" else "Sign Up",
                modifier = Modifier.clickable { 
                    isSignUp = !isSignUp
                    viewModel.resetState()
                    viewModel.clearFieldErrors()
                },
                color = LogoBlueDark,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Reset Password", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Enter your email or username and we'll send you instructions.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = forgotPasswordId,
                        onValueChange = { forgotPasswordId = it },
                        label = { Text("Email or Username") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                val toastMessage by viewModel.toastMessage.collectAsState()
                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearToastMessage()
                    }
                }
                
                TextButton(onClick = {
                    if (forgotPasswordId.isNotBlank()) {
                        viewModel.forgotPassword(forgotPasswordId)
                        showForgotPassword = false
                    }
                }) {
                    Text("Send", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPassword = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun ValidationText(text: String?) {
    if (text != null) {
        Text(
            text = text,
            color = Color.Red,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
        )
    }
}
