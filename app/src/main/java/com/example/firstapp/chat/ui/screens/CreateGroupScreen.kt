package com.example.firstapp.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.ui.viewmodel.CreateGroupViewModel
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    selectedContacts: List<Contact>,
    onBack: () -> Unit,
    onRemoveContact: (Contact) -> Unit = {},
    onCreated: (ChatMessage) -> Unit,
    viewModel: CreateGroupViewModel
) {
    var groupName by remember { mutableStateOf("") }
    val createResult by viewModel.createResult.collectAsState()
    val isCreating = createResult is Resource.Loading

    LaunchedEffect(createResult) {
        if (createResult is Resource.Success) {
            createResult?.data?.let { onCreated(it) }
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New Group", fontWeight = FontWeight.Bold)
                        Text("Add subject", fontSize = 12.sp, color = Color.Gray)
                    }
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
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    color = AppSecondary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray)
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                TextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Type group subject here...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = AppAccent,
                        unfocusedIndicatorColor = Color.Gray
                    )
                )
            }
            
            Text(
                "Participants: ${selectedContacts.size}",
                modifier = Modifier.padding(16.dp),
                color = AppAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(selectedContacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(contact.displayName.take(1).uppercase(), color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(contact.displayName, color = Color.White, modifier = Modifier.weight(1f))
                        
                        IconButton(onClick = { onRemoveContact(contact) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray)
                        }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { 
                        viewModel.createGroup(groupName, selectedContacts.map { it.id })
                    },
                    enabled = groupName.isNotBlank() && !isCreating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                ) {
                    if (isCreating) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = AppBackground)
                    else Text("CREATE GROUP", color = AppBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
