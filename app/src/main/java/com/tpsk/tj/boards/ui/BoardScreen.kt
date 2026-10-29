package com.tpsk.tj.boards.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.tpsk.tj.boards.data.*
import com.tpsk.tj.media.data.api.MediaApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class BoardCopy(
    val title: String,
    val noun: String,
    val priceHint: String,
    val action: String,
    val yourHeader: String,
    val filters: List<String>
)

fun boardCopy(board: String) = when (board) {
    "rent" -> BoardCopy(
        title = "Rooms available",
        noun = "room",
        priceHint = "R3,500 / month",
        action = "Ask about this room",
        yourHeader = "Your rooms",
        filters = listOf("Nearest", "Under R1500", "With Parking")
    )
    "services" -> BoardCopy(
        title = "Handyman services",
        noun = "service",
        priceHint = "From R250",
        action = "Request this service",
        yourHeader = "Your services",
        filters = listOf("Nearest", "Plumbers", "Electricians")
    )
    else -> BoardCopy(
        title = "Second-hand market",
        noun = "item",
        priceHint = "R150",
        action = "Message seller",
        yourHeader = "Your listings",
        filters = listOf("Nearby (5km)", "Appliances", "Clothes")
    )
}

@HiltViewModel
class BoardViewModel @Inject constructor(
    private val api: BoardApi,
    private val mediaApi: MediaApi
) : ViewModel() {
    private val _items = MutableStateFlow<List<BoardListing>>(emptyList())
    val items: StateFlow<List<BoardListing>> = _items
    private val _mine = MutableStateFlow<List<BoardListing>>(emptyList())
    val mine: StateFlow<List<BoardListing>> = _mine
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message
    var board = "market"

    fun load(query: String = "") {
        viewModelScope.launch {
            runCatching { api.list(board, query.ifBlank { null }) }.onSuccess { response ->
                _items.value = response.body()?.listings.orEmpty()
                _message.value = if (!response.isSuccessful) "Could not load listings" else ""
            }.onFailure { _message.value = "Could not load listings" }
            runCatching { api.mine(board) }.onSuccess { _mine.value = it.body()?.listings.orEmpty() }
        }
    }

    fun publish(title: String, description: String, price: String, area: String, photo: String) {
        viewModelScope.launch {
            val response = runCatching {
                api.create(board, CreateListingRequest(title, description, price, area, photo))
            }.getOrNull()
            _message.value = if (response?.isSuccessful == true) "Published" else "Sign in and add a title before posting"
            load()
        }
    }

    fun close(id: String) {
        viewModelScope.launch {
            api.update(board, id, UpdateListingRequest("closed"))
            load()
        }
    }

    fun interest(id: String, note: String, onChat: (String) -> Unit) {
        viewModelScope.launch {
            val response = runCatching { api.interest(board, id, InterestRequest(note)) }.getOrNull()
            val chat = response?.body()?.conversationId.orEmpty()
            if (response?.isSuccessful == true && chat.isNotBlank()) onChat(chat)
            else _message.value = "Could not open chat. Sign in and try again."
        }
    }

    fun upload(context: Context, uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                _message.value = "Could not read that photo"
                return@launch
            }
            val type = context.contentResolver.getType(uri) ?: "image/jpeg"
            val part = MultipartBody.Part.createFormData("file", "listing.jpg", bytes.toRequestBody(type.toMediaTypeOrNull()))
            val folder = "portal".toRequestBody("text/plain".toMediaTypeOrNull())
            val response = runCatching { mediaApi.uploadMedia(part, folder) }.getOrNull()
            val url = response?.body()?.url.orEmpty()
            if (response?.isSuccessful == true && url.isNotBlank()) onDone(url)
            else _message.value = "Photo upload failed"
        }
    }
}

@Composable
fun RentScreen(
    onBack: () -> Unit,
    onChat: (String) -> Unit,
    viewModel: BoardViewModel = hiltViewModel()
) {
    BoardScreen(board = "rent", onBack = onBack, onChat = onChat, viewModel = viewModel)
}

@Composable
fun ServicesScreen(
    onBack: () -> Unit,
    onChat: (String) -> Unit,
    viewModel: BoardViewModel = hiltViewModel()
) {
    BoardScreen(board = "services", onBack = onBack, onChat = onChat, viewModel = viewModel)
}

@Composable
fun MarketScreen(
    onBack: () -> Unit,
    onChat: (String) -> Unit,
    viewModel: BoardViewModel = hiltViewModel()
) {
    BoardScreen(board = "market", onBack = onBack, onChat = onChat, viewModel = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardScreen(
    board: String,
    onBack: () -> Unit,
    onChat: (String) -> Unit,
    viewModel: BoardViewModel = hiltViewModel()
) {
    val copy = boardCopy(board)
    val items by viewModel.items.collectAsState()
    val mine by viewModel.mine.collectAsState()
    val message by viewModel.message.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(0) }
    var posting by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<BoardListing?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.upload(context, uri) { photo = it }
    }

    LaunchedEffect(board) {
        viewModel.board = board
        viewModel.load()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(copy.title, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { posting = !posting }) {
                        Text(
                            text = if (posting) "Close" else "Post",
                            color = Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar
            item(span = { GridItemSpan(2) }) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.load(query)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    placeholder = { Text("Search area or title", color = Color(0xFF9CA3AF)) },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A242D),
                        unfocusedContainerColor = Color(0xFF1A242D),
                        focusedBorderColor = Color(0xFF2A3847),
                        unfocusedBorderColor = Color(0xFF2A3847)
                    )
                )
            }

            // Filter Chips Bar
            item(span = { GridItemSpan(2) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(copy.filters.size) { index ->
                        val filter = copy.filters[index]
                        val isSelected = selectedFilter == index
                        Surface(
                            modifier = Modifier.clickable {
                                selectedFilter = index
                                viewModel.load(if (index == 0) query else filter)
                            },
                            shape = CircleShape,
                            color = if (isSelected) Color(0xFF0F4C45) else Color(0xFF1A242D),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00D2B4))
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3847))
                            }
                        ) {
                            Text(
                                text = "[ $filter ]",
                                color = if (isSelected) Color(0xFF00D2B4) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (message.isNotBlank()) {
                item(span = { GridItemSpan(2) }) {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }

            // Post Creation Section
            if (posting) {
                item(span = { GridItemSpan(2) }) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18222C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("New ${copy.noun.replaceFirstChar { it.uppercase() }} Listing", fontWeight = FontWeight.Bold)
                            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("${copy.noun.replaceFirstChar { it.uppercase() }} title") })
                            OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), label = { Text("Details") }, minLines = 3)
                            OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text(copy.priceHint) })
                            OutlinedTextField(area, { area = it }, Modifier.fillMaxWidth(), label = { Text("Area") })
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { picker.launch("image/*") }) { Text(if (photo.isBlank()) "Add photo" else "Photo added") }
                                Button(
                                    onClick = {
                                        viewModel.publish(title.trim(), details.trim(), price.trim(), area.trim(), photo)
                                        posting = false
                                        title = ""; details = ""; price = ""; area = ""; photo = ""
                                    },
                                    enabled = title.isNotBlank()
                                ) { Text("Publish") }
                            }
                        }
                    }
                }
            }

            // Your Listings
            if (mine.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = copy.yourHeader,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(mine, key = { "mine-${it.id}" }) { item ->
                    ListingCard(
                        item = item,
                        action = "Mark unavailable",
                        onActionClick = { viewModel.close(item.id) },
                        onClick = { selected = item }
                    )
                }
            }

            // Available Listings
            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Available",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (items.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text("Nothing is listed yet. Be the first to post.", color = Color(0xFF9CA3AF))
                }
            } else {
                items(items, key = { it.id }) { item ->
                    ListingCard(
                        item = item,
                        onClick = { selected = item }
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    selected?.let { item ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(item.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.photoUrl.isNotBlank()) AsyncImage(item.photoUrl, item.title, Modifier.fillMaxWidth().height(160.dp))
                    Text("${item.priceLabel} · ${item.area}")
                    Text(item.description)
                    Text(item.sellerName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(note, { note = it.take(300) }, Modifier.fillMaxWidth(), label = { Text("Message") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.interest(item.id, note.ifBlank { "Hi, I'm interested in ${item.title}" }) { chatId ->
                        selected = null
                        onChat(chatId)
                    }
                }) { Text(copy.action) }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun ListingCard(
    item: BoardListing,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18222C))
    ) {
        Column {
            if (item.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = item.photoUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(125.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(125.dp)
                        .background(Color(0xFF24303D))
                )
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.priceLabel.isNotBlank()) {
                    Text(
                        text = item.priceLabel,
                        color = Color(0xFF00D2B4),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1
                )
                val subtitle = listOfNotNull(
                    item.sellerName.ifBlank { null },
                    item.area.ifBlank { null }
                ).joinToString(" · ").ifBlank { "Gauteng" }
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF9CA3AF),
                    maxLines = 1
                )
                if (action != null && onActionClick != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = action,
                        color = Color(0xFF3B82F6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onActionClick() }
                    )
                }
            }
        }
    }
}
