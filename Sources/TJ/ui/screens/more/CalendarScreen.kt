package com.tpsk.tj.ui.screens.more

import androidx.hilt.navigation.compose.hiltViewModel

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.common.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpsk.tj.common.ui.viewmodel.CalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit, viewModel: CalendarViewModel = hiltViewModel()) {
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar", color = AppTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Add event */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event", tint = AppAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            CalendarHeader(
                month = currentMonth,
                onPreviousMonth = {
                    currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                },
                onNextMonth = {
                    currentMonth = (currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                }
            )

            CalendarGrid(month = currentMonth)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Upcoming Events",
                color = AppTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            val events = listOf(
                CalendarEvent("Team Sync", "10:00 AM", "Office", Color(0xFF2196F3)),
                CalendarEvent("Client Call", "1:30 PM", "Zoom", Color(0xFF4CAF50)),
                CalendarEvent("Design Review", "4:00 PM", "Meeting Room B", Color(0xFFFF9800))
            )

            LazyColumn {
                items(events) { event ->
                    EventItem(event)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
fun CalendarHeader(
    month: Calendar,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color.White)
        }
        Text(
            text = monthName,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
        }
    }
}

@Composable
fun CalendarGrid(month: Calendar) {
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        val rows = (daysInMonth + firstDayOfWeek + 6) / 7
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                repeat(7) { col ->
                    val dayNum = row * 7 + col - firstDayOfWeek + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                if (dayNum == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) && 
                                    month.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) 
                                    AppAccent.copy(alpha = 0.2f) else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            Text(
                                text = dayNum.toString(),
                                color = if (dayNum == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) && 
                                    month.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) 
                                    AppAccent else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (dayNum == Calendar.getInstance().get(Calendar.DAY_OF_MONTH)) 
                                    FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventItem(event: CalendarEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(event.color)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("${event.time} • ${event.location}", color = AppTextSecondary, fontSize = 13.sp)
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

data class CalendarEvent(val title: String, val time: String, val location: String, val color: Color)
