package com.tpsk.tj.drive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.tpsk.tj.drive.model.Trip
import com.tpsk.tj.common.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveHistoryScreen(
    onBack: () -> Unit,
    trips: List<Trip> = emptyList()
) {
    BackHandler { onBack() }
    var selectedTrip by remember { mutableStateOf<Trip?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Rides", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSecondary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (trips.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.DirectionsCar, 
                                contentDescription = null, 
                                tint = Color.White.copy(0.1f), 
                                modifier = Modifier.size(120.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "No trips yet", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 20.sp
                            )
                            Text(
                                "Your ride history will appear here", 
                                color = AppTextSecondary, 
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(trips) { trip ->
                    Surface(
                        onClick = { selectedTrip = trip },
                        color = AppSecondary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).background(AppAccent.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = AppAccent)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trip.date, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${trip.from} → ${trip.to}", color = AppTextSecondary, fontSize = 13.sp, maxLines = 1)
                                Text(trip.car, color = AppTextSecondary, fontSize = 12.sp)
                            }
                            Text(trip.price, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    selectedTrip?.let { trip ->
        TripDetailDialog(trip = trip, onDismiss = { selectedTrip = null })
    }
}

@Composable
fun TripDetailDialog(trip: Trip, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = { Text("Trip Details", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailRow("Date", trip.date)
                DetailRow("From", trip.from)
                DetailRow("To", trip.to)
                DetailRow("Vehicle", trip.car.ifBlank { "TJ Ride" })
                DetailRow("Distance", trip.distance.ifBlank { "—" })
                DetailRow("Fare", "R ${"%.2f".format(trip.fare)}")
                DetailRow("TJ fee (20%)", "R ${"%.2f".format(trip.platformFee)}")
                DetailRow(if (trip.role == "driver") "You received" else "You paid", trip.price)
                DetailRow("Paid with", trip.paymentMethod.ifBlank { "Wallet" })
                DetailRow("Status", trip.status, color = if (trip.status == "Completed") Color(0xFF4CAF50) else Color.White)
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        val receipt = """
                            TJ Ride receipt
                            ${trip.date}
                            From: ${trip.from}
                            To: ${trip.to}
                            Fare: R ${"%.2f".format(trip.fare)}
                            TJ fee: R ${"%.2f".format(trip.platformFee)}
                            ${if (trip.role == "driver") "Driver earning" else "Paid"}: ${trip.price}
                            Payment: ${trip.paymentMethod.ifBlank { "Wallet" }}
                            Status: ${trip.status}
                        """.trimIndent()
                        context.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, receipt)
                        }, "Share receipt"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share receipt", color = Color.White)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AppAccent)
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String, color: Color = Color.White) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = AppTextSecondary, fontSize = 14.sp)
        Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}
