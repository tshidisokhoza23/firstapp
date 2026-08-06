package com.example.firstapp.auth.ui.screens

import android.content.Context
import android.content.IntentFilter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.firstapp.auth.ui.viewmodel.AuthViewModel
import com.example.firstapp.auth.util.SmsBroadcastReceiver
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.AppSignatureHelper
import com.example.firstapp.common.util.Resource
import com.google.android.gms.auth.api.phone.SmsRetriever

@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    username: String? = null,
    email: String? = null,
    displayName: String? = null,
    password: String? = null,
    countryCode: String? = null,
    onVerificationSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {

    var otpCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    val otpState by viewModel.otpState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val smsReceiver = remember { SmsBroadcastReceiver() }

    val isRegistration = username != null && password != null

    DisposableEffect(Unit) {
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(smsReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(smsReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null)
        }

        smsReceiver.setListener(object : SmsBroadcastReceiver.SmsBroadcastReceiverListener {
            override fun onSuccess(message: String) {
                val regex = kotlin.text.Regex("(\\d{6})")
                val matchResult = regex.find(message)
                if (matchResult != null) {
                    otpCode = matchResult.groupValues[0]
                }
            }
            override fun onFailure() {}
        })

        onDispose {
            try {
                context.unregisterReceiver(smsReceiver)
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(authState) {
        if (authState is Resource.Success) {
            onVerificationSuccess()
        }
    }

    fun verify() {
        if (otpCode.length == 6) {
            viewModel.verifyOTP(phoneNumber, otpCode) {
                if (isRegistration) {
                    viewModel.register(username!!, email ?: "", displayName ?: "", phoneNumber, password!!, countryCode)
                } else {
                    viewModel.commitLogin()
                }
            }
        }
    }


    LaunchedEffect(otpCode) {
        if (otpCode.length == 6) {
            verify()
        }
    }

    val errorMessage = (otpState as? Resource.Error)?.message ?: (authState as? Resource.Error)?.message
    val isLoading = otpState is Resource.Loading || authState is Resource.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Verify Phone",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Enter the 6-digit code sent to your registered number",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage, 
                color = MaterialTheme.colorScheme.error, 
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = otpCode,
            onValueChange = { input ->
                if (input.length <= 6 && input.all { char: Char -> char.isDigit() }) {
                    otpCode = input
                    if (input.length == 6) {
                        verify()
                    }
                }
            },
            label = { Text("6-Digit Code") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { verify() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            enabled = !isLoading && otpCode.length == 6
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp), 
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (isRegistration) "Verify & Sign Up" else "Verify & Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val appHash = remember {
                val helper = AppSignatureHelper(context)
                val sigs = helper.getAppSignatures()
                if (sigs.size > 0) sigs[0] else ""
            }
            Text(
                text = "Hash: $appHash",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("App Hash", appHash)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Hash copied", Toast.LENGTH_SHORT).show()
                }
            )

            TextButton(onClick = {
                val client = SmsRetriever.getClient(context)
                client.startSmsRetriever()
                viewModel.requestOTP(phoneNumber, countryCode)
            }) {
                Text("Resend Code", color = MaterialTheme.colorScheme.primary)
            }

        }
        
        TextButton(onClick = onBack) {
            Text("Edit Phone Number", color = MaterialTheme.colorScheme.primary)
        }
    }
}
