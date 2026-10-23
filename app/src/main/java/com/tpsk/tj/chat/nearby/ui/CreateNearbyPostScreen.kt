@file:OptIn(ExperimentalMaterial3Api::class)

package com.tpsk.tj.chat.nearby.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tpsk.tj.LocalCurrentUser
import com.tpsk.tj.chat.nearby.ui.viewmodel.NearbyViewModel

@Composable
fun CreateNearbyPostScreen(
    initialArea: String = "",
    onBack: () -> Unit,
    onPostCreated: () -> Unit,
    nearbyViewModel: NearbyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUser = LocalCurrentUser.current
    val uiState by nearbyViewModel.uiState.collectAsState()

    var postContent by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf(initialArea.ifBlank { uiState.coarseAreaName }) }
    var attachedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isPublishing by remember { mutableStateOf(false) }
    var showAreaPickerSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            attachedImages = (attachedImages + uris).take(4)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (postContent.trim().isBlank() || isPublishing) return@Button
                            isPublishing = true
                            nearbyViewModel.createPostWithImages(
                                content = postContent,
                                authorId = currentUser?.id.orEmpty(),
                                authorName = currentUser?.displayName ?: currentUser?.username ?: "Neighbor",
                                authorAvatarUrl = currentUser?.avatarUrl,
                                areaName = selectedArea,
                                imageUrls = attachedImages.map { it.toString() },
                                onSuccess = {
                                    isPublishing = false
                                    Toast.makeText(context, "Post published!", Toast.LENGTH_SHORT).show()
                                    onPostCreated()
                                },
                                onError = { err ->
                                    isPublishing = false
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        enabled = postContent.trim().isNotBlank() && !isPublishing,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Publish", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Location Selector Header Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF00B4D8).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF00B4D8).copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { showAreaPickerSheet = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF00B4D8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedArea.ifBlank { "📍 Select Area" },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00B4D8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF00B4D8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Borderless Auto-Expanding Text Input
                OutlinedTextField(
                    value = postContent,
                    onValueChange = { if (it.length <= 500) postContent = it },
                    placeholder = {
                        Text(
                            "What's happening around you? Ask questions, share local events, or find lost items...",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Attached Image Previews
                if (attachedImages.isNotEmpty()) {
                    Text(
                        text = "Attached Photos (${attachedImages.size}/4)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(attachedImages) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Attachment",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clickable { attachedImages = attachedImages - uri }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action Bar (Docked above keyboard)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color(0xFF00B4D8)
                        )
                    }

                    IconButton(
                        onClick = { showAreaPickerSheet = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocation,
                            contentDescription = "Pin Location",
                            tint = Color(0xFF00B4D8)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "${postContent.length}/500",
                        fontSize = 12.sp,
                        color = if (postContent.length >= 500) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showAreaPickerSheet) {
        AreaPickerSheet(
            currentArea = selectedArea,
            onDismiss = { showAreaPickerSheet = false },
            onAreaSelected = { area ->
                selectedArea = area
                showAreaPickerSheet = false
            }
        )
    }
}

@Composable
fun AreaPickerSheet(
    currentArea: String,
    onDismiss: () -> Unit,
    onAreaSelected: (String) -> Unit
) {
    val areas = remember {
        listOf(
            "Ecaleni, Tembisa", "Mqantsa, Tembisa", "Sangweni, Tembisa", "Birchfield",
            "Vaal", "Soweto", "Midrand", "Johannesburg", "Pretoria", "Centurion", "Kempton Park", "Benoni"
        )
    }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) areas
        else areas.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Select Location / Area",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search town or township...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF00B4D8).copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAreaSelected("Ecaleni, Tembisa") }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF00B4D8))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Use Current Location",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00B4D8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            filtered.forEach { area ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAreaSelected(area) }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = area,
                        fontSize = 15.sp,
                        fontWeight = if (area == currentArea) FontWeight.Bold else FontWeight.Normal,
                        color = if (area == currentArea) Color(0xFF00B4D8) else MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}
