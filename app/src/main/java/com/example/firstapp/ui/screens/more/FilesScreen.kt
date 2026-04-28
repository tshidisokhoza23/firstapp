package com.example.firstapp.ui.screens.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.ui.theme.AppAccent
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.AppSecondary
import com.example.firstapp.ui.theme.AppTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Files", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSecondary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        val files = listOf(
            FileItem("Documents", true, "124 items"),
            FileItem("Images", true, "1,042 items"),
            FileItem("Videos", true, "48 items"),
            FileItem("resume_v2.pdf", false, "1.2 MB"),
            FileItem("project_spec.docx", false, "450 KB"),
            FileItem("budget_2024.xlsx", false, "89 KB")
        )

        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Internal Storage", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("85% full", color = AppTextSecondary, fontSize = 12.sp)
                }
                LinearProgressIndicator(
                    progress = { 0.85f },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(4.dp).clip(CircleShape),
                    color = AppAccent,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(files) { file ->
                ListItem(
                    headlineContent = { Text(file.name, color = Color.White, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(file.info, color = AppTextSecondary, fontSize = 12.sp) },
                    leadingContent = {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = (if (file.isFolder) AppAccent else Color.Gray).copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (file.isFolder) Icons.Default.Folder else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (file.isFolder) AppAccent else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = AppTextSecondary)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

data class FileItem(val name: String, val isFolder: Boolean, val info: String)
