package com.example.firstapp.chat.channels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.media.ui.viewmodel.ChannelsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelScreen(
    onBack: () -> Unit,
    onCreated: () -> Unit,
    viewModel: ChannelsViewModel
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val toastMessage by viewModel.toastMessage.collectAsState()

    LaunchedEffect(toastMessage) {
        if (toastMessage == "Channel created successfully") {
            onCreated()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("New Channel", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    color = AppSecondary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Text(
                "Channel Name",
                color = AppAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Enter channel name", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppSecondary,
                    unfocusedContainerColor = AppSecondary,
                    focusedIndicatorColor = AppAccent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                "Description (optional)",
                color = AppAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            TextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("What is this channel about?", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AppSecondary,
                    unfocusedContainerColor = AppSecondary,
                    focusedIndicatorColor = AppAccent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = MaterialTheme.shapes.medium
            )
            
            Text(
                "Anyone can discover your channel and see the last 30 days of history.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { viewModel.createChannel(name, description) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Create Channel", color = AppBackground, fontWeight = FontWeight.Bold)
            }
        }
    }
}
