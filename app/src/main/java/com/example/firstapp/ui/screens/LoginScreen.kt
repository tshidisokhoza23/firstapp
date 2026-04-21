@file:Suppress("DEPRECATION")

package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.AppSecondary
import com.example.firstapp.ui.theme.AppTextPrimary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.api.AuthResponse
import com.example.firstapp.data.api.RegisterRequest
import com.example.firstapp.data.api.LoginRequest
import com.example.firstapp.data.api.ApiClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.Response
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var username by remember { mutableStateOf("alice") }
    var password by remember { mutableStateOf("password123") }
    var displayName by remember { mutableStateOf("Test User") }
    var phoneNumber by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {
                        onLoginSuccess()
                    } else {
                        errorMessage = "Firebase authentication failed"
                    }
                }
        } catch (e: ApiException) {
            errorMessage = "Google sign in failed: ${e.message}"
        }
    }

    fun performGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("303703683011-g2p7atop0uackslr2eq9fnsja5jlfk7u.apps.googleusercontent.com")
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    fun performLogin() {
        val cleanUsername = username.trim()
        val cleanDisplayName = displayName.trim()
        val cleanPhone = phoneNumber.trim()
        val cleanPassword = password

        if (cleanUsername.isBlank() || cleanPassword.isBlank()) {
            errorMessage = "Please fill all fields"
            return
        }
        if (isSignUp && (cleanDisplayName.isBlank() || cleanPhone.isBlank())) {
            errorMessage = "All fields are required for sign up"
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            try {
                val response: Response<AuthResponse> = if (isSignUp) {
                    ApiClient.apiService.register(
                        RegisterRequest(cleanUsername, cleanDisplayName, cleanPhone, cleanPassword)
                    )
                } else {
                    ApiClient.apiService.login(
                        LoginRequest(cleanUsername, cleanPassword)
                    )
                }

                if (response.isSuccessful) {
                    response.body()?.let { authResponse ->
                        TokenManager.setToken(authResponse.token)
                        authResponse.refreshToken.takeIf { it.isNotBlank() }?.let { TokenManager.setRefreshToken(it) }
                        onLoginSuccess()
                    }
                } else {
                    val apiMessage = try {
                        response.errorBody()?.string()?.let { raw ->
                            JSONObject(raw).optString("error").ifBlank { raw }
                        }
                    } catch (_: Exception) {
                        null
                    }
                    errorMessage = apiMessage ?: "Request failed (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = e.toLoginMessage()
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
                text = errorMessage!!,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it; errorMessage = null },
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
                value = displayName,
                onValueChange = { displayName = it; errorMessage = null },
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

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it; errorMessage = null },
                label = { Text("Phone Number") },
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
        }

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
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

        Spacer(modifier = Modifier.height(32.dp))

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
                Text(if (isSignUp) "Sign Up" else "Login", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppBackground)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = { isSignUp = !isSignUp; errorMessage = null }) {
            Text(
                text = if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up",
                color = AppAccent,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = AppSecondary)
            Text(" OR ", color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = AppSecondary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                performGoogleSignIn()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // You'll need a google icon in your res/drawable
                Icon(
                    imageVector = Icons.Default.Person, // Temporary icon
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Continue with Google", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private fun Throwable.toLoginMessage(): String {
    return when (this) {
        is ConnectException,
        is UnknownHostException,
        is SocketTimeoutException -> "Can't reach the server. Check that the backend is running and try again."
        else -> localizedMessage?.takeIf { it.isNotBlank() }
            ?.let { "Network error: $it" }
            ?: "Sign in failed. Please try again."
    }
}
