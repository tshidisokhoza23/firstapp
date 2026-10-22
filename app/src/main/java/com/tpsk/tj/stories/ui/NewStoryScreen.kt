@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.tpsk.tj.stories.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun NewStoryScreen(
    onBack: () -> Unit,
    onNext: (content: String, url: String, type: String) -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf("image") }
    var hasLaunchedPicker by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri)
            mediaType = if (mimeType?.startsWith("video") == true) "video" else "image"
            selectedUri = uri
        } else {
            if (selectedUri == null) {
                onBack()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLaunchedPicker) {
            hasLaunchedPicker = true
            launcher.launch("*/*")
        }
    }

    selectedUri?.let { uri ->
        com.tpsk.tj.chat.ui.screens.MediaPreviewOverlay(
            uris = listOf(uri),
            type = mediaType,
            onDismiss = onBack,
            onSend = { caption, processedUris ->
                val finalUri = processedUris.firstOrNull() ?: uri
                onNext(caption, finalUri.toString(), mediaType)
            }
        )
    }
}
