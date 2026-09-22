package com.tpsk.tj.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.common.ui.theme.AppAccent
import com.tpsk.tj.common.ui.theme.AppBackground
import com.tpsk.tj.common.ui.theme.AppSecondary
import com.tpsk.tj.common.ui.theme.AppTextSecondary
import com.tpsk.tj.common.ui.viewmodel.FilesViewModel
import com.tpsk.tj.chat.model.MessageType
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.common.ui.components.DataStateHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(onBack: () -> Unit, viewModel: FilesViewModel = hiltViewModel()) {
    val filesState by viewModel.files.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = java.util.Arrays.asList("All", "Images", "Videos", "Documents", "Audio")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Files", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground, titleContentColor = Color.White)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Category filter
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                containerColor = AppBackground,
                contentColor = AppAccent,
                edgePadding = 16.dp,
                divider = {}
            ) {
                for (category in categories) {
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        text = { Text(category) }
                    )
                }
            }

            DataStateHandler(
                resource = filesState
            ) { messages: List<Message> ->
                val filteredFiles = java.util.ArrayList<Message>()
                for (msg in messages) {
                    val include = when (selectedCategory) {
                        "Images" -> msg.type == MessageType.IMAGE
                        "Videos" -> msg.type == MessageType.VIDEO
                        "Documents" -> msg.type == MessageType.DOCUMENT
                        "Audio" -> msg.type == MessageType.AUDIO_MESSAGE
                        else -> true
                    }
                    if (include) {
                        filteredFiles.add(msg)
                    }
                }

                if (filteredFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No files found", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredFiles) { msg ->
                            FileItem(
                                fileName = msg.fileName ?: "File",
                                fileSize = msg.fileSize ?: "",
                                timestamp = msg.timestamp.toString(), // Simplified
                                type = msg.type
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileItem(fileName: String, fileSize: String, timestamp: String, type: MessageType) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (type) {
            MessageType.IMAGE -> Icons.Default.Image
            MessageType.VIDEO -> Icons.Default.VideoFile
            MessageType.AUDIO_MESSAGE -> Icons.Default.AudioFile
            MessageType.DOCUMENT -> Icons.Default.Description
            else -> Icons.Default.Description
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AppAccent)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$fileSize • $timestamp",
                color = AppTextSecondary,
                fontSize = 12.sp
            )
        }

        IconButton(onClick = { /* Download or open */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.Gray)
        }
    }
}
