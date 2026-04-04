package com.example.firstapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.Destination
import com.example.firstapp.model.RideOption
import com.example.firstapp.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

@Composable
fun DriveScreen() {
    var step by remember { mutableIntStateOf(0) } // 0: Home, 1: Selection, 2: Booking
    var isSearching by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<Destination?>(null) }
    var selectedRide by remember { mutableStateOf<RideOption?>(null) }

    val sandton = LatLng(-26.1076, 28.0567)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(sandton, 13f)
    }

    val rideOptions = listOf(
        RideOption(1, "FirstApp Go", "R45.00", "3 mins", "Affordable, compact rides", Color(0xFF4CAF50)),
        RideOption(2, "FirstApp Comfort", "R65.00", "5 mins", "Newer cars with extra legroom", Color(0xFF2196F3)),
        RideOption(3, "FirstApp Premium", "R120.00", "4 mins", "Luxury vehicles & top-rated drivers", Color(0xFF9C27B0)),
        RideOption(4, "FirstApp XL", "R85.00", "7 mins", "SUVs for up to 6 people", Color(0xFFFF9800))
    )

    val recentDestinations = listOf(
        Destination(1, "Home", "123 Innovation Drive, Sandton", Color(0xFF4CAF50)),
        Destination(2, "Office", "Building 5, Tech Park, Midrand", Color(0xFF2196F3)),
        Destination(3, "Mall of Africa", "Lone Creek Cres, Waterval City", Color(0xFFFFC107))
    )

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Real Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = false, // Set to true if you have permissions
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            // Simulated driver/destination markers
            Marker(
                state = MarkerState(position = sandton),
                title = "Your Location",
                snippet = "Current Location"
            )
            
            if (selectedDestination != null) {
                Marker(
                    state = MarkerState(position = LatLng(-26.12, 28.08)),
                    title = selectedDestination?.name ?: "Destination",
                    icon = null // You can use BitmapDescriptorFactory for custom icons
                )
            }
        }

        // Overlay UI
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppBackground.copy(alpha = 0.8f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0 || isSearching) {
                        IconButton(onClick = { 
                            if (isSearching) isSearching = false else step-- 
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                    }
                    Text(
                        text = when {
                            isSearching -> "Search Destination"
                            step == 0 -> "FirstApp Drive"
                            step == 1 -> "Select Ride"
                            else -> "Arriving in 2 mins"
                        },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Interactive Bottom Panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                color = AppSecondary,
                shadowElevation = 12.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    when {
                        isSearching || (step == 0 && selectedDestination == null) -> {
                            // Search Interface
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSearching = true },
                                placeholder = { Text("Where to?", color = AppTextSecondary) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppAccent) },
                                enabled = true,
                                readOnly = !isSearching,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color(0xFF374045),
                                    focusedBorderColor = AppAccent,
                                    disabledBorderColor = Color(0xFF374045)
                                ),
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            )

                            // Show recent destinations ONLY when searching or if selectedDestination is null
                            AnimatedVisibility(
                                visible = isSearching || step == 0,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Column(modifier = Modifier.padding(top = 20.dp)) {
                                    Text("Recent Places", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    recentDestinations.forEach { dest ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedDestination = dest
                                                    isSearching = false
                                                    step = 1
                                                }
                                                .padding(vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier.size(44.dp).background(dest.iconColor.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.History, contentDescription = null, tint = dest.iconColor, modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(dest.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text(dest.address, color = AppTextSecondary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        step == 1 -> {
                            // Ride Selection Screen
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Destination: ", color = AppTextSecondary)
                                Text(selectedDestination?.name ?: "", color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("Edit", color = AppAccent, modifier = Modifier.clickable { step = 0; selectedDestination = null })
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            LazyColumn(modifier = Modifier.height(280.dp)) {
                                items(rideOptions) { ride ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { selectedRide = ride },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedRide?.id == ride.id) Color(0xFF233138) else Color.Transparent
                                        ),
                                        border = if (selectedRide?.id == ride.id) androidx.compose.foundation.BorderStroke(1.dp, AppAccent) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(52.dp).background(ride.iconColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(ride.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                Text("${ride.arrivalTime} away", color = AppAccent, fontSize = 13.sp)
                                            }
                                            Text(ride.price, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }
                            
                            Button(
                                onClick = { step = 2 },
                                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 12.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                                enabled = selectedRide != null
                            ) {
                                Text("Book ${selectedRide?.name ?: ""}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                        step == 2 -> {
                            // Driver Tracking Screen
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(64.dp).background(Color(0xFF455A64), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Sipho M.", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                    Text("Toyota Corolla • KPV 123 GP", color = AppTextSecondary, fontSize = 14.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                                        Text(" 4.9 · Gold Partner", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                                Box(
                                    modifier = Modifier.size(48.dp).background(Color(0xFF374045), CircleShape).clickable { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, tint = AppAccent)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF233138),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        LinearProgressIndicator(
                                            modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape),
                                            color = AppAccent,
                                            trackColor = AppBackground
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("2 mins", color = AppAccent, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Driver is arriving at your pickup location", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = { step = 0; selectedDestination = null; selectedRide = null },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Text("Cancel Trip")
                                }
                                Button(
                                    onClick = {},
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF374045))
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Message")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
