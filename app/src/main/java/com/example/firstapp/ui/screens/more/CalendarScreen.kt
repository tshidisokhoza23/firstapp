package com.example.firstapp.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.ui.theme.AppBackground
import com.example.firstapp.ui.theme.AppSecondary
import com.example.firstapp.ui.theme.AppTextPrimary
import com.example.firstapp.ui.theme.AppTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", color = AppTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Placeholder for a calendar view
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp)
                    .background(AppSecondary, shape = MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Text("May 2024", color = AppTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "Upcoming Events",
                color = AppTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            val events = listOf(
                CalendarEvent("Team Meeting", "10:00 AM - 11:00 AM", "Office"),
                CalendarEvent("Lunch with Sarah", "12:30 PM - 1:30 PM", "Cafe"),
                CalendarEvent("Product Launch", "3:00 PM - 4:30 PM", "Online")
            )

            LazyColumn {
                items(events) { event ->
                    ListItem(
                        headlineContent = { Text(event.title, color = AppTextPrimary) },
                        supportingContent = { Text("${event.time} | ${event.location}", color = AppTextSecondary) },
                        leadingContent = {
                            Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFFE91E63))
                        },
                        colors = ListItemDefaults.colors(containerColor = AppBackground)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }
            }
        }
    }
}

data class CalendarEvent(val title: String, val time: String, val location: String)
