package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.ui.theme.*

data class DriveTrip(
    val id: Int,
    val date: String,
    val from: String,
    val to: String,
    val price: String,
    val car: String,
    val status: String = "Completed"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveHistoryScreen(onBack: () -> Unit) {
    val trips = listOf(
        DriveTrip(1, "Today, 10:24", "123 Innovation Drive", "Tech Park Building", "R85.50", "Toyota Corolla"),
        DriveTrip(2, "Yesterday, 18:45", "Mall of Africa", "123 Innovation Drive", "R120.00", "VW Polo"),
        DriveTrip(3, "15 Oct, 08:12", "Home", "Gym", "R45.00", "BMW 3 Series")
    )

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
            items(trips) { trip ->
                Surface(
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
