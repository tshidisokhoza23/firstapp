package com.example.firstapp.stories.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.activity.result.PickVisualMediaRequest
import com.example.firstapp.common.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewStoryScreen(
    onBack: () -> Unit,
    onNext: (content: String, url: String, type: String) -> Unit
) {
    val context = LocalContext.current
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var mediaType by remember { mutableStateOf("text") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val mimeType = context.contentResolver.getType(it)
            val isVideo = mimeType?.startsWith("video") == true
            
            selectedMediaUri = it
            mediaType = if (isVideo) "video" else "image"
        }
    }

    val editorLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { selectedMediaUri = it }
        }
    }

    var videoDurationMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(selectedMediaUri) {
        if (selectedMediaUri != null && mediaType == "video") {
            videoDurationMs = com.example.firstapp.common.util.MediaUtils.getVideoDuration(context, selectedMediaUri!!)
        } else {
            videoDurationMs = 0L
        }
    }

    val isVideoTooLong = mediaType == "video" && videoDurationMs > 120000L

    LaunchedEffect(Unit) {
        if (selectedMediaUri == null) {
            launcher.launch("*/*")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("New Story", color = Color.White)
                        if (isVideoTooLong) {
                            Text("Video exceeds 2m limit", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (selectedMediaUri != null) {
                        IconButton(onClick = { 
                            try {
                                val intent = Intent(Intent.ACTION_EDIT).apply {
                                    setDataAndType(selectedMediaUri, if (mediaType == "video") "video/*" else "image/*")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                editorLauncher.launch(intent)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "No editor found on device.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Media", tint = if (isVideoTooLong) Color.Red else Color.White)
                        }
                    }
                    IconButton(onClick = { /* Download logic */ }) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Add a caption...", color = Color.LightGray) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    
                    IconButton(onClick = { /* Timer logic */ }) {
                        Icon(Icons.Default.Timer, contentDescription = "Timer", tint = Color.White)
                    }
                    IconButton(onClick = { /* More logic */ }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val onEdit = {
                            if (selectedMediaUri != null) {
                                try {
                                    val intent = Intent(Intent.ACTION_EDIT).apply {
                                        setDataAndType(selectedMediaUri, if (mediaType == "video") "video/*" else "image/*")
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                        editorLauncher.launch(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No editor found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        IconButton(onClick = onEdit) { Icon(Icons.Default.Brush, null, tint = Color.White) }
                        IconButton(onClick = onEdit) { Icon(Icons.Default.Face, null, tint = Color.White) }
                        IconButton(onClick = onEdit) { Icon(Icons.Default.TextFields, null, tint = Color.White) }
                        IconButton(onClick = onEdit) { Icon(Icons.Default.Tune, null, tint = Color.White) }
                    }
                    
                    Button(
                        onClick = { 
                            if (isVideoTooLong) {
                                android.widget.Toast.makeText(context, "Please trim video to under 2 minutes first.", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                onNext(caption, selectedMediaUri?.toString() ?: "", mediaType)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVideoTooLong) Color.Gray else MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(if (isVideoTooLong) "TRIM REQUIRED" else "NEXT", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (selectedMediaUri != null) {
                AsyncImage(
                    model = selectedMediaUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                if (isVideoTooLong) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Warning, null, tint = Color.Yellow, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Video is too long", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("${videoDurationMs / 1000}s selected (Max 120s)", color = Color.LightGray, fontSize = 14.sp)
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = { 
                                        try {
                                            val intent = Intent(Intent.ACTION_EDIT).apply {
                                                setDataAndType(selectedMediaUri, "video/*")
                                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                            }
                                            editorLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "No video editor found", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.Edit, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("TRIM VIDEO")
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF8D6E63), Color(0xFF4E342E)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select media or start typing...", color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}
