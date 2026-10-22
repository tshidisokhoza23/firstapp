package com.tpsk.tj.media.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import coil.compose.AsyncImage

data class EventItem(
    val id: String,
    val title: String,
    val timeAndLocation: String,
    val imageUrl: String,
    val ticketBadge: String,
    val interestedText: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onBack: () -> Unit,
    onShareToChat: (EventItem) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Today", "This Weekend", "Campus", "Concerts", "Sports")

    val eventsList = remember {
        listOf(
            EventItem(
                id = "event_1",
                title = "Johannesburg Tech & Innovation Summit",
                timeAndLocation = "Saturday, 24 Oct • 12:00 • Sandton Convention Centre, JHB",
                imageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800",
                ticketBadge = "R50 Tickets",
                interestedText = "500+ interested",
                category = "Campus"
            ),
            EventItem(
                id = "event_2",
                title = "Gauteng Artisanal Food & Craft Market",
                timeAndLocation = "Sunday, 25 Oct • 10:00 • Waterfall Park, Midrand",
                imageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800",
                ticketBadge = "Free Entry",
                interestedText = "120+ interested",
                category = "This Weekend"
            ),
            EventItem(
                id = "event_3",
                title = "Live Jazz & Afrobeat Festival",
                timeAndLocation = "Friday, 30 Oct • 18:00 • Rosebank Park",
                imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
                ticketBadge = "R120 Tickets",
                interestedText = "350+ interested",
                category = "Concerts"
            )
        )
    }

    val filteredEvents = remember(selectedCategory) {
        if (selectedCategory == "All") eventsList
        else eventsList.filter { it.category.equals(selectedCategory, ignoreCase = true) || selectedCategory == "All" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Events", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
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
        ) {
            // Horizontal Category Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFF00B4D8) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Events List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredEvents, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onShareToChat = {
                            onShareToChat(event)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "${event.title}\n${event.timeAndLocation}\nTicket: ${event.ticketBadge}\nCheck it out on TJ Events!")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share to Chat"))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EventCard(
    event: EventItem,
    onShareToChat: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image with top-right ticket badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                AsyncImage(
                    model = event.imageUrl,
                    contentDescription = event.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Top-Right Ticket Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(
                        text = event.ticketBadge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Title
                Text(
                    text = event.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Time & Location
                Text(
                    text = event.timeAndLocation,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Row: Avatar stack + interested count & "Share to Chat ➔"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar stack preview
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        listOf(Color(0xFF00B4D8), Color(0xFFE91E63), Color(0xFF4CAF50)).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = event.interestedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Cyan "Share to Chat ➔" Button
                    Button(
                        onClick = onShareToChat,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Share to Chat",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
