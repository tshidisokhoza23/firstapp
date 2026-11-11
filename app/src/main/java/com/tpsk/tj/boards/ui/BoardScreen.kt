package com.tpsk.tj.boards.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.tpsk.tj.media.data.api.MediaApi
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.boards.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val api: BoardApi,
    private val mediaApi: MediaApi
) : ViewModel() {
    private val _items = MutableStateFlow<List<BoardListing>>(emptyList())
    val items: StateFlow<List<BoardListing>> = _items.asStateFlow()
    private val _mine = MutableStateFlow<List<BoardListing>>(emptyList())
    val mine: StateFlow<List<BoardListing>> = _mine.asStateFlow()
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()
    var board = ""

    fun load(q: String = "") {
        viewModelScope.launch {
            _message.value = ""
            try {
                val res = api.list(board, q.ifBlank { null }).body()
                val mineRes = api.mine(board).body()
                _items.value = res?.listings.orEmpty()
                _mine.value = mineRes?.listings.orEmpty()
            } catch (e: Exception) {
                _message.value = "Couldn't load $board"
                
                // MOCK DATA
                if (board == "rent") {
                    _items.value = listOf(
                        BoardListing("r1", "user1", "Room to rent in Tembisa", "Single Room, Wi-Fi", "1200", "Tembisa, Gauteng • 1.2 km", "", "active", "contact1"),
                        BoardListing("r2", "user2", "Shared room available", "Bunk beds, Parking", "1000", "Tembisa, Gauteng • 2.5 km", "", "active", "contact2"),
                        BoardListing("r3", "user3", "Spacious room for rent", "En-suite, Wi-Fi", "1800", "Tembisa, Gauteng • 3.1 km", "", "active", "contact3")
                    )
                } else if (board == "market") {
                    _items.value = listOf(
                        BoardListing("m1", "user1", "Samsung Galaxy S24", "Like New", "7500", "Tembisa • 1.2 km", "", "active", "contact1"),
                        BoardListing("m2", "user2", "Hisense Washing Machine", "Good", "2800", "Tembisa • 1.5 km", "", "active", "contact2"),
                        BoardListing("m3", "user3", "Nike Air Force 1", "Like New", "1200", "Tembisa • 2.1 km", "", "active", "contact3"),
                        BoardListing("m4", "user4", "Comfortable Sofa", "Good", "3000", "Tembisa • 3.0 km", "", "active", "contact4")
                    )
                } else if (board == "services") {
                    _items.value = listOf(
                        BoardListing("s1", "Thabo Plumbing", "Emergency plumbing", "Burst Pipes, Leak Repairs", "From R250", "Tembisa, Gauteng • 1.2 km", "", "active", "contact1"),
                        BoardListing("s2", "Mokoena Electrical", "Home electrical repairs", "Fault Finding, Repairs", "Call-out from R300", "Tembisa, Gauteng • 2.1 km", "", "active", "contact2"),
                        BoardListing("s3", "Kasi Paint Pros", "Painting & wall repairs", "Interior, Exterior", "Quotes available", "Tembisa, Gauteng • 3.0 km", "", "active", "contact3")
                    )
                }
            }
        }
    }

    fun publish(title: String, description: String, price: String, area: String, photo: String) {
        viewModelScope.launch {
            try {
                api.create(board, CreateListingRequest(title, description, price, area, photo))
                load()
            } catch (e: Exception) {
                _message.value = "Failed to post"
            }
        }
    }

    fun close(id: String) {
        viewModelScope.launch {
            try {
                api.update(board, id, UpdateListingRequest("closed"))
                load()
            } catch (e: Exception) {
                _message.value = "Failed to close post"
            }
        }
    }

    fun interest(id: String, note: String, onStart: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val res = api.interest(board, id, InterestRequest(note)).body()
                if (res != null && res.conversationId.isNotBlank()) onStart(res.conversationId)
            } catch (e: Exception) {
                _message.value = "Couldn't send message"
            }
        }
    }
}

val HeaderColor = Color(0xFF0B0F3A)
val AccentColor = Color(0xFF583EF2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentScreen(
    onBack: () -> Unit,
    onChat: (String) -> Unit,
    onPostClick: () -> Unit,
    viewModel: BoardViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    var query by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.board = "rent"
        viewModel.load()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rooms available", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = onPostClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 12.dp).height(36.dp)
                    ) {
                        Text("+ Post", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderColor)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search area or title...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    trailingIcon = { Icon(Icons.Default.Tune, contentDescription = "Filter", tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedBorderColor = AccentColor, unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    singleLine = true
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    item {
                        FilterChip(
                            selected = true, onClick = {}, label = { Text("Nearest ∨", color = Color.White) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentColor)
                        )
                    }
                    item {
                        FilterChip(
                            selected = false, onClick = {}, label = { Text("Under R1500 ∨") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false, onClick = {}, label = { Text("With Photos") },
                            leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
            
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFE8EAF6), Color(0xFFC5CAE9))))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.6f)) {
                        Surface(color = AccentColor, shape = RoundedCornerShape(12.dp)) {
                            Text("Sponsored", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Find Your Perfect Room", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HeaderColor)
                        Text("Rooms, shared spaces and more near you.", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                            shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Explore Now →", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)) {
                            Icon(Icons.Outlined.FavoriteBorder, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(item.area, color = Color.Gray, fontSize = 12.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                item.description.split(",").take(2).forEach {
                                    Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(4.dp)) {
                                        Text(it.trim(), fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("R${item.priceLabel} /month", fontWeight = FontWeight.Bold, color = AccentColor)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.ChevronRight, null, tint = AccentColor)
                        }
                    }
                }
            }
            
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.6f)) {
                        Surface(color = Color(0xFF4CAF50), shape = RoundedCornerShape(12.dp)) {
                            Text("Sponsored", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Advertise Your Room", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1B5E20))
                        Text("Reach more people in your area.", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Get Started →", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    onBack: () -> Unit,
    onChat: (String) -> Unit,
    onPostClick: () -> Unit,
    viewModel: BoardViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    var query by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        viewModel.board = "market"
        viewModel.load()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Second-hand market", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White) }
                        Icon(Icons.Default.LocalMall, null, tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = onPostClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 12.dp).height(36.dp)
                    ) {
                        Text("+ Post", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderColor)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search for items...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    trailingIcon = { Icon(Icons.Default.Tune, contentDescription = "Filter", tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedBorderColor = AccentColor, unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    singleLine = true
                )
            }
            
            item(span = { GridItemSpan(2) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = true, onClick = {}, label = { Text("Nearby (5km) ∨", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentColor)
                        )
                    }
                    val categories = listOf("Appliances", "Clothes", "Electronics", "Furniture")
                    items(categories) { cat ->
                        FilterChip(selected = false, onClick = {}, label = { Text(cat) })
                    }
                }
            }
            
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(HeaderColor, AccentColor)))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.7f)) {
                        Text("Buy • Sell • Both Win", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("Find great deals or sell your items easily!", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                            shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Start Selling →", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
            
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color.LightGray)) {
                            Icon(Icons.Outlined.FavoriteBorder, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp))
                            Surface(
                                color = AccentColor, shape = RoundedCornerShape(topEnd = 8.dp),
                                modifier = Modifier.align(Alignment.BottomStart)
                            ) {
                                Text("For Sale", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(item.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("R${item.priceLabel}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccentColor)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(item.area, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Icon(Icons.Default.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Recently", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
            
            item(span = { GridItemSpan(2) }) {
                Surface(
                    color = Color.White, shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, null, tint = AccentColor, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Safe & trusted community", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                            Text("Meet in a safe place and always check the item.", color = Color.Gray, fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    onChat: (String) -> Unit,
    onPostClick: () -> Unit,
    viewModel: BoardViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    var query by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.board = "services"
        viewModel.load()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Handyman services", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = onPostClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 12.dp).height(36.dp)
                    ) {
                        Text("+ Post", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = HeaderColor)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search area or service", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                    trailingIcon = { Icon(Icons.Default.Tune, contentDescription = "Filter", tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedBorderColor = AccentColor, unfocusedBorderColor = Color(0xFFE5E7EB)
                    ),
                    singleLine = true
                )
            }
            
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    item {
                        FilterChip(
                            selected = true, onClick = {}, label = { Text("Nearest ∨", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentColor)
                        )
                    }
                    val trades = listOf("Plumbers", "Electricians", "Painters", "Carpenters", "Welding")
                    items(trades) { t ->
                        FilterChip(
                            selected = false, onClick = {}, label = { Text(t) },
                            leadingIcon = { Icon(Icons.Default.Build, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }
            
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFEDE7F6), Color(0xFFD1C4E9))))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.6f)) {
                        Surface(color = AccentColor, shape = RoundedCornerShape(12.dp)) {
                            Text("Sponsored", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Get trusted help around you", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HeaderColor)
                        Text("Reliable, skilled and affordable handyman services.", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                            shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Find a pro →", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            
            items(items, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray)) {
                            Icon(Icons.Outlined.FavoriteBorder, null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(item.area, color = Color.Gray, fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(item.sellerName, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                Text(" 4.8 (124 reviews)", fontSize = 11.sp, color = Color.Gray)
                            }
                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                item.description.split(",").forEach {
                                    Surface(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(4.dp)) {
                                        Text(it.trim(), fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0821234567"))
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentColor),
                                        border = BorderStroke(1.dp, AccentColor),
                                        modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Call", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {},
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                                        border = BorderStroke(1.dp, Color.LightGray),
                                        modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text("View Details →", fontSize = 12.sp)
                                    }
                                }
                                Text(item.priceLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentColor)
                            }
                        }
                    }
                }
            }
            
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8EAF6))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(0.6f)) {
                        Surface(color = AccentColor, shape = RoundedCornerShape(12.dp)) {
                            Text("Sponsored", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Promote your services", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HeaderColor)
                        Text("Reach more people in your area.", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                            shape = RoundedCornerShape(8.dp), modifier = Modifier.height(32.dp), contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text("Get Started →", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
