package com.example.firstapp.drive.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.drive.model.Destination
import com.example.firstapp.drive.model.DestinationType
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.drive.ui.viewmodel.DriveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesScreen(
    onBack: () -> Unit,
    viewModel: DriveViewModel
) {
    BackHandler { onBack() }
    val destinationsState by viewModel.destinations.collectAsState()
    val favorites = remember(destinationsState) {
        destinationsState.data?.filter { it.isFavorite || it.type == DestinationType.HOME || it.type == DestinationType.WORK } ?: emptyList()
    }

    var showAddPlace by remember { mutableStateOf(false) }
    var placeToEdit by remember { mutableStateOf<Destination?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Places", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddPlace = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = AppAccent)
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Primary Addresses",
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val home = favorites.find { it.type == DestinationType.HOME }
            val work = favorites.find { it.type == DestinationType.WORK }

            item {
                SavedPlaceItem(
                    icon = Icons.Default.Home,
                    name = "Home",
                    address = home?.address ?: "Set home address",
                    iconColor = Color(0xFF4CAF50),
                    onClick = { placeToEdit = home ?: Destination(0, "Home", "", type = DestinationType.HOME) }
                )
            }
            item {
                SavedPlaceItem(
                    icon = Icons.Default.Work,
                    name = "Work",
                    address = work?.address ?: "Set work address",
                    iconColor = Color(0xFF2196F3),
                    onClick = { placeToEdit = work ?: Destination(0, "Work", "", type = DestinationType.WORK) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Other Favorites",
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val otherFavorites = favorites.filter { it.type == DestinationType.FAVORITE || (it.isFavorite && it.type == DestinationType.RECENT) }
            
            if (otherFavorites.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No other favorites saved", color = Color.White.copy(alpha = 0.3f))
                    }
                }
            } else {
                items(otherFavorites) { place ->
                    SavedPlaceItem(
                        icon = Icons.Default.Star,
                        name = place.name,
                        address = place.address,
                        iconColor = NiceOrange,
                        onClick = { placeToEdit = place },
                        onDelete = { viewModel.deleteDestination(place.id) }
                    )
                }
            }
        }
    }

    if (showAddPlace) {
        AddSavedPlaceDialog(onDismiss = { showAddPlace = false }, viewModel = viewModel)
    }

    placeToEdit?.let { place ->
        EditSavedPlaceDialog(
            destination = place,
            onDismiss = { placeToEdit = null },
            viewModel = viewModel
        )
    }
}

@Composable
fun SavedPlaceItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    address: String,
    iconColor: Color,
    onClick: () -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        color = AppSecondary,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(address, color = AppTextSecondary, fontSize = 13.sp, maxLines = 1)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White.copy(alpha = 0.3f))
                }
            } else {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AppTextSecondary)
            }
        }
    }
}

@Composable
fun AddSavedPlaceDialog(onDismiss: () -> Unit, viewModel: DriveViewModel) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = { Text("Add Favorite", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Mom's House)", color = AppTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = Color.White.copy(0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address", color = AppTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = Color.White.copy(0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotBlank() && address.isNotBlank()) {
                        viewModel.saveDestination(name, address, isFavorite = true, type = "favorite")
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
            ) {
                Text("Save", color = AppBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTextSecondary)
            }
        }
    )
}

@Composable
fun EditSavedPlaceDialog(destination: Destination, onDismiss: () -> Unit, viewModel: DriveViewModel) {
    var name by remember { mutableStateOf(destination.name) }
    var address by remember { mutableStateOf(destination.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = { 
            val typeName = destination.type.name.lowercase().replaceFirstChar { it.uppercase() }
            Text("Edit $typeName", color = Color.White, fontWeight = FontWeight.Bold) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (destination.type == DestinationType.FAVORITE || destination.type == DestinationType.RECENT) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name", color = AppTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppAccent,
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address", color = AppTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = Color.White.copy(0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (address.isNotBlank()) {
                        viewModel.saveDestination(name, address, isFavorite = destination.isFavorite, type = destination.type.name.lowercase())
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
            ) {
                Text("Update", color = AppBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTextSecondary)
            }
        }
    )
}
