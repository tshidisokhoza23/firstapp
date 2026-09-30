@file:Suppress("DEPRECATION")

package com.tpsk.tj.auth.ui.screens

import kotlin.text.Regex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.shadow
import com.tpsk.tj.R
import com.tpsk.tj.common.ui.theme.*
import android.widget.Toast


import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.tpsk.tj.common.data.api.ForgotPasswordRequest
import java.net.ConnectException

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpsk.tj.auth.ui.viewmodel.AuthViewModel
import com.tpsk.tj.auth.ui.components.CountryPickerField
import com.tpsk.tj.auth.util.SmsBroadcastReceiver
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.common.util.CountryUtils
import com.tpsk.tj.common.util.Country
import com.tpsk.tj.common.util.AppSignatureHelper
import com.google.android.gms.auth.api.phone.SmsRetriever
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException

@Composable
fun LoginScreen(
    onLoginSuccess: (com.tpsk.tj.common.data.api.AuthResponse?) -> Unit,
    onNavigateToOtp: (String, String?, String?, String?, String?, String?) -> Unit,
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
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        selectedCountry = CountryUtils.getDefaultCountry(context)
    }

    LaunchedEffect(authState) {
        val state = authState
        if (state is Resource.Success) {
            onLoginSuccess(state.data)
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
                onNavigateToOtp(phone, username.trim(), email.trim(), displayName.trim(), password, selectedCountry?.dialCode)
            } else {
                onNavigateToOtp(phone, null, null, null, null, selectedCountry?.dialCode)
            }
            viewModel.resetOtpState()
        }
    }


    fun performLogin() {
        Toast.makeText(context, "Phone and manual login is temporarily disabled. Please sign in with Google.", Toast.LENGTH_SHORT).show()
    }


    val backgroundBrush = if (LocalIsDarkTheme.current) {
        Brush.verticalGradient(
            colors = listOf(AppBackgroundDarkTop, AppBackgroundDarkBottom)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color.White, AppBackgroundLight)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {



        Spacer(modifier = Modifier.height(60.dp))
        
        // Logo Container
        Surface(
            modifier = Modifier
                .size(120.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                com.tpsk.tj.home.ui.components.AppLogo(size = 90.dp)
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
            text = if (isSignUp) "Join TJ today" else "Login to continue",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "by TPSK",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
        )

        
        Spacer(modifier = Modifier.height(48.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Info Banner for Google Sign-In
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    Toast.makeText(context, "Manual login & phone registration is temporarily disabled. Please tap 'Continue with Google'.", Toast.LENGTH_SHORT).show()
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign in & sign up are currently supported via Google. Tap below to continue.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Phone/Username Field (Disabled)
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = username,
                enabled = false,
                onValueChange = { 
                    username = it
                    viewModel.setFieldError("username", null)
                    viewModel.resetState()
                },
                label = { Text(if (isSignUp) "Username" else "Phone Number") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSignUp) Icons.Default.Person else Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                isError = fieldErrors.containsKey("username"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
            ValidationText(fieldErrors["username"])
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSignUp) {
            // Email Field (Disabled)
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = email,
                    enabled = false,
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
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
                ValidationText(fieldErrors["email"])
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Display Name (Disabled)
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = displayName,
                    enabled = false,
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
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                        enabled = false,
                        onValueChange = { input ->
                            if (input.all { char: Char -> char.isDigit() || char == '+' }) {
                                phoneNumber = input
                            }
                            viewModel.setFieldError("phone", null)
                        },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g. 821234567 or 0821234567") },
                        supportingText = { Text("Supports 9/10 digits or country code", fontSize = 11.sp) },
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
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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


        // Password Field (Disabled)
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = password,
                enabled = false,
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
                    disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
            ValidationText(fieldErrors["password"])
        }

        if (isSignUp) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = confirmPassword,
                    enabled = false,
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
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
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
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Gradient Primary Google Sign In Button
        val gradientBrush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF1A237E), Color(0xFF00D1FF))
        )

        Button(
            onClick = {
                scope.launch {
                    val nonce = java.util.UUID.randomUUID().toString()
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId("303703683011-g2p7atop0uackslr2eq9fnsja5jlfk7u.apps.googleusercontent.com")
                        .setNonce(nonce)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    try {
                        Log.d("LoginScreen", "Starting GetCredentialRequest...")
                        val result = credentialManager.getCredential(context, request)
                        val credential = result.credential
                        Log.d("LoginScreen", "Received credential type: ${credential::class.java.simpleName}")
                        
                        if (credential is GoogleIdTokenCredential) {
                            Log.d("LoginScreen", "Calling viewModel.googleSignIn...")
                            viewModel.googleSignIn(credential.idToken)
                        } else if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            try {
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                Log.d("LoginScreen", "Parsed GoogleIdTokenCredential from CustomCredential")
                                viewModel.googleSignIn(googleIdTokenCredential.idToken)
                            } catch (e: GoogleIdTokenParsingException) {
                                Log.e("LoginScreen", "Received invalid google id token response", e)
                            }
                        } else {
                            Log.w("LoginScreen", "Received unexpected credential type: ${credential.type}")
                        }
                    } catch (e: GetCredentialCancellationException) {
                        Log.d("LoginScreen", "Google Sign In Cancelled")
                    } catch (e: GetCredentialException) {
                        Log.e("LoginScreen", "Google Sign In Failed", e)
                        Toast.makeText(context, "Google Sign In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(gradientBrush, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            enabled = !isLoading,
            contentPadding = PaddingValues()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Registration & phone login disabled. ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Text(
                text = "Use Google",
                modifier = Modifier.clickable { 
                    Toast.makeText(context, "Registration and login are managed automatically via Google. Please tap 'Continue with Google'.", Toast.LENGTH_SHORT).show()
                },
                color = LogoBlueDark,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        }
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
