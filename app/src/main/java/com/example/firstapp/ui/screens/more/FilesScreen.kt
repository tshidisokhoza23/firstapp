package com.example.firstapp.ui.screens.more

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.theme.AppTextSecondary

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.common.ui.viewmodel.FilesViewModel

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.firstapp.chat.model.MessageType
import com.example.firstapp.common.util.Resource
import com.example.firstapp.home.ui.screens.DataStateHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(onBack: () -> Unit, viewModel: FilesViewModel = hiltViewModel()) {
    val filesState by viewModel.files.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Images", "Videos", "Documents", "Audio")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground, titleContentColor = Color.White)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            LazyRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppAccent,
                            selectedLabelColor = AppBackground,
                            labelColor = AppTextSecondary
                        )
                    )
                }
            }

            DataStateHandler(
                resource = filesState,
                onSuccess = { messages ->
                    val filteredFiles = messages.filter { msg ->
                        when (selectedCategory) {
                            "Images" -> msg.type == MessageType.IMAGE
                            "Videos" -> msg.type == MessageType.VIDEO
                            "Documents" -> msg.type == MessageType.DOCUMENT
                            "Audio" -> msg.type == MessageType.AUDIO_MESSAGE
                            else -> msg.type != MessageType.TEXT
                        }
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredFiles) { msg ->
                            FileItemRow(
                                fileName = msg.fileName ?: "Unknown file",
                                fileSize = msg.fileSize ?: "Unknown size",
                                timestamp = msg.timestamp.toString(), // Simplified
                                type = msg.type
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun FileItemRow(fileName: String, fileSize: String, timestamp: String, type: MessageType) {
    val icon = when (type) {
        MessageType.IMAGE -> Icons.Default.Image
        MessageType.VIDEO -> Icons.Default.VideoFile
        MessageType.DOCUMENT -> Icons.Default.Description
        MessageType.AUDIO_MESSAGE -> Icons.Default.AudioFile
        else -> Icons.Default.FilePresent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(AppSecondary, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AppAccent)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(fileName, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$fileSize • $timestamp", color = AppTextSecondary, fontSize = 12.sp)
        }
        IconButton(onClick = { }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = AppTextSecondary)
        }
    }
}
