package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
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
import com.example.firstapp.model.Contact
import com.example.firstapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    selectedContacts: List<Contact>,
    onBack: () -> Unit,
    onCreate: (String, List<Int>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New group", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Add subject", fontSize = 12.sp, color = AppTextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSecondary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        floatingActionButton = {
            if (groupName.isNotBlank()) {
                FloatingActionButton(
                    onClick = { onCreate(groupName, selectedContacts.map { it.id }) },
                    containerColor = AppAccent,
                    contentColor = AppBackground,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Done")
                }
            }
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                TextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Type group subject here...", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = AppAccent,
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            Text(
                text = "Participants: ${selectedContacts.size}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = AppTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(selectedContacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AppAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = AppBackground)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(contact.displayName, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
