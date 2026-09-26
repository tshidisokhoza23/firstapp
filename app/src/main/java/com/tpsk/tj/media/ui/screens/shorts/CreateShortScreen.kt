package com.tpsk.tj.media.ui.screens.shorts

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.tpsk.tj.media.data.worker.ShortUploadWorker
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateShortScreen(
    initialVideoUri: Uri? = null,
    onBackClick: () -> Unit,
    onPostSuccess: () -> Unit,
    mediaViewModel: MediaViewModel
) {
    val context = LocalContext.current
    var selectedVideoUri by remember { mutableStateOf<Uri?>(initialVideoUri) }
    var caption by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var allowComments by remember { mutableStateOf(true) }
    var confirmedRights by remember { mutableStateOf(true) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            // File Size Check (Max 250MB)
            val pfd = try { context.contentResolver.openFileDescriptor(uri, "r") } catch (_: Exception) { null }
            val size = pfd?.statSize ?: 0L
            pfd?.close()

            if (size > 250 * 1024 * 1024) {
                Toast.makeText(context, "Video exceeds 250MB size limit", Toast.LENGTH_LONG).show()
            } else {
                selectedVideoUri = uri
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Short", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val uri = selectedVideoUri
                            if (uri == null) {
                                Toast.makeText(context, "Please select a video clip", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!confirmedRights) {
                                Toast.makeText(context, "Please confirm content guidelines consent", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // 1. Trigger background WorkManager pipeline
                            val localId = UUID.randomUUID().toString()
                            val uploadRequest = OneTimeWorkRequestBuilder<ShortUploadWorker>()
                                .setConstraints(
                                    Constraints.Builder()
                                        .setRequiredNetworkType(NetworkType.CONNECTED)
                                        .setRequiresStorageNotLow(true)
                                        .build()
                                )
                                .setInputData(
                                    workDataOf(
                                        ShortUploadWorker.KEY_LOCAL_ID to localId,
                                        ShortUploadWorker.KEY_URI to uri.toString(),
                                        ShortUploadWorker.KEY_DESCRIPTION to caption
                                    )
                                )
                                .build()

                            WorkManager.getInstance(context).enqueue(uploadRequest)

                            // 2. Submit post creation
                            mediaViewModel.createPost(
                                content = caption,
                                mediaUris = listOf(uri.toString()),
                                postType = "short",
                                mediaType = "video"
                            )

                            // 3. Instant Pop back to Shorts feed
                            onPostSuccess()
                        },
                        enabled = selectedVideoUri != null && confirmedRights,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Post Short", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Video Preview Card / Selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { videoPickerLauncher.launch("video/*") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (selectedVideoUri == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Tap to select a video clip (max 250MB)", fontWeight = FontWeight.SemiBold)
                            Text("Recommended: 9:16 portrait video (1080x1920)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = selectedVideoUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(Icons.Default.PlayCircle, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Video Ready", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Ready for compression & upload", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { videoPickerLauncher.launch("video/*") },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Change Video")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Caption Text Field
            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 500) caption = it },
                label = { Text("Caption & #hashtags") },
                placeholder = { Text("Write an engaging caption for your short...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(16.dp),
                supportingText = {
                    Text("${caption.length}/500", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )

            Spacer(Modifier.height(16.dp))

            // Settings Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Public, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Visibility", fontWeight = FontWeight.SemiBold)
                        }
                        Switch(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Allow Comments", fontWeight = FontWeight.SemiBold)
                        }
                        Switch(
                            checked = allowComments,
                            onCheckedChange = { allowComments = it }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Legal Rights Consent Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { confirmedRights = !confirmedRights }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = confirmedRights,
                    onCheckedChange = { confirmedRights = it }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "I confirm I own or have permission to publish this video and agree to Community Guidelines.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
