@file:Suppress("DEPRECATION")

package com.example.firstapp.auth.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
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
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.theme.AppTextPrimary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import android.widget.Toast
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.firstapp.common.data.api.ApiClient
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
    onNavigateToOtp: (String, String, String, String, String) -> Unit,
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

    LaunchedEffect(otpState) {
        if (otpState is Resource.Success) {
            val phone = viewModel.lastRequestedPhone
            if (isSignUp) {
                onNavigateToOtp(phone, username.trim(), email.trim(), displayName.trim(), password)
            } else {
                onNavigateToOtp(phone, "NONE", "NONE", "NONE", "NONE")
            }
            viewModel.resetOtpState()
        }
    }

    fun performLogin() {
        val cleanUsername = username.trim()
        val cleanEmail = email.trim()
        val cleanDisplayName = displayName.trim()
        val rawPhone = phoneNumber.trim()
        val cleanPassword = password

        val cleanPhone = if (rawPhone.startsWith("+")) {
            rawPhone
        } else {
            val dialCode = selectedCountry?.dialCode ?: ""
            val formatted = if (rawPhone.startsWith("0")) rawPhone.substring(1) else rawPhone
            dialCode + formatted
        }

        if (cleanUsername.isBlank() || cleanPassword.isBlank()) {
            Toast.makeText(context, "Username and password are required", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isSignUp) {
            if (cleanDisplayName.isBlank() || cleanPhone.isBlank() || cleanEmail.isBlank()) {
                Toast.makeText(context, "All fields are required for sign up", Toast.LENGTH_SHORT).show()
                return
            }

            // Email Validation
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                return
            }

            // Phone Validation and Formatting
            if (cleanPhone.length < 10) {
                Toast.makeText(context, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
                return
            }

            if (password != confirmPassword) {
                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return
            }

            // Password strength check (min 8 chars)
            if (password.length < 8) {
                Toast.makeText(context, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show()
                return
            }

            // Request OTP
            val client = SmsRetriever.getClient(context)
            client.startSmsRetriever()
            viewModel.requestOTP(cleanPhone)
        } else {
            viewModel.login(cleanUsername, cleanPassword)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = AppTextPrimary
        )
        Text(
            text = if (isSignUp) "Join FirstApp today" else "Login to continue",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; viewModel.resetState() },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppAccent,
                unfocusedBorderColor = AppSecondary,
                focusedLabelColor = AppAccent,
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; viewModel.resetState() },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppAccent,
                    unfocusedBorderColor = AppSecondary,
                    focusedLabelColor = AppAccent,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it; viewModel.resetState() },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppAccent,
                    unfocusedBorderColor = AppSecondary,
                    focusedLabelColor = AppAccent,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                        viewModel.resetState() 
                    },
                    label = { Text("Phone Number") },
                    placeholder = { Text("e.g. 812345678") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = AppSecondary,
                        focusedLabelColor = AppAccent,
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; viewModel.resetState() },
            label = { Text("Password") },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppAccent,
                unfocusedBorderColor = AppSecondary,
                focusedLabelColor = AppAccent,
                unfocusedLabelColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        if (isSignUp) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; viewModel.resetState() },
                label = { Text("Confirm Password") },
                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                        Icon(
                            imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppAccent,
                    unfocusedBorderColor = AppSecondary,
                    focusedLabelColor = AppAccent,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }

        if (!isSignUp) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = { showForgotPassword = true }) {
                    Text("Forgot Password?", color = AppTextPrimary.copy(alpha = 0.6f), fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { performLogin() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AppBackground, strokeWidth = 2.dp)
            } else {
                val buttonText = if (!isSignUp) "Login" else "Send Code"
                Text(buttonText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppBackground)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = { 
            isSignUp = !isSignUp
            viewModel.resetState()
        }) {
            Text(
                text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                color = AppAccent,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            containerColor = AppSecondary,
            title = { Text("Reset Password", color = Color.White) },
            text = {
                Column {
                    Text("Enter your email or username and we'll send you instructions.", color = Color.White.copy(0.7f))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = forgotPasswordId,
                        onValueChange = { forgotPasswordId = it },
                        label = { Text("Email or Username") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppAccent,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (forgotPasswordId.isNotBlank()) {
                        scope.launch {
                            try {
                                ApiClient.authApi.forgotPassword(ForgotPasswordRequest(forgotPasswordId))
                                Toast.makeText(context, "Reset link sent!", Toast.LENGTH_SHORT).show()
                            } catch (_: Exception) {}
                        }
                        showForgotPassword = false
                    }
                }) {
                    Text("Send", color = AppAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPassword = false }) {
                    Text("Cancel", color = Color.White.copy(0.6f))
                }
            }
        )
    }
}
