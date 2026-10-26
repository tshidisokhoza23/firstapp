@file:Suppress("DEPRECATION")

package com.tpsk.tj.auth.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.tpsk.tj.R
import com.tpsk.tj.auth.ui.viewmodel.AuthViewModel
import com.tpsk.tj.common.util.GlobalNavigation
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.ui.navigation.Screen
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (com.tpsk.tj.common.data.api.AuthResponse?) -> Unit,
    onNavigateToOtp: (String, String?, String?, String?, String?, String?) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    val otpState by viewModel.otpState.collectAsState()
    val credentialManager = remember { CredentialManager.create(context) }

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

    fun performGoogleSignIn() {
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
    }

    TJWelcomeScreen(
        onContinueWithGoogle = { performGoogleSignIn() },
        onTermsClick = {
            GlobalNavigation.navigateTo(Screen.TermsAndPrivacy.createRoute())
        },
        isLoading = isLoading
    )
}

@Composable
fun TJWelcomeScreen(
    onContinueWithGoogle: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    isLoading: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF0A2A6B),   // deep blue top
                        0.35f to Color(0xFF0077E6),  // mid blue
                        0.7f to Color(0xFF00C4E0),   // cyan
                        1.0f to Color(0xFF0099CC)    // bottom blue
                    )
                )
            )
    ) {
        // Subtle diagonal light shape
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x2200E5FF),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1200f, 1800f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            // TJ Logo
            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo_main),
                        contentDescription = "TJ Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(22.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(56.dp))

            // Main title
            Text(
                text = "Welcome to TJ",
                style = TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = "By continuing you accept the terms\nand conditions",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Continue with Google button
            Button(
                onClick = onContinueWithGoogle,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4A90E2),
                                    Color(0xFF5B9FEA),
                                    Color(0xFF6BB0F0)
                                )
                            ),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Bottom terms
            Text(
                text = "Terms and conditions",
                modifier = Modifier.clickable { onTermsClick() },
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.85f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
