package com.example.firstapp.auth.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.auth.ui.viewmodel.AuthViewModel
import com.example.firstapp.common.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteProfileScreen(
    onCompleteSuccess: () -> Unit,
    viewModel: AuthViewModel
) {
    val currentUser = com.example.firstapp.LocalCurrentUser.current
    val authState by viewModel.authState.collectAsState()

    var displayName by remember { mutableStateOf(currentUser.displayName) }
    var username by remember { mutableStateOf(currentUser.username) }
    var phoneNumber by remember { mutableStateOf(currentUser.phoneNumber) }

    val missingFields = currentUser.missingFields ?: emptyList()

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

            if (missingFields.contains("display_name") || currentUser.displayName.isEmpty()) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            if (missingFields.contains("username") || currentUser.username.isEmpty()) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. john_doe") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            if (missingFields.contains("phone_number") || currentUser.phoneNumber.isEmpty()) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("+27...") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            if (authState is Resource.Error) {
                Text(
                    text = (authState as Resource.Error).message ?: "Error",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = {
                    viewModel.completeProfile(
                        displayName.takeIf { it != currentUser.displayName },
                        username.takeIf { it != currentUser.username },
                        phoneNumber.takeIf { it != currentUser.phoneNumber }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = authState !is Resource.Loading
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
