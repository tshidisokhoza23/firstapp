package com.tpsk.tj.boards.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

data class BoardCopy(val title: String, val noun: String, val priceHint: String, val action: String)

fun boardCopy(board: String) = when (board) {
    "rent" -> BoardCopy("Rooms available", "room", "R3,500 / month", "Ask about this room")
    "services" -> BoardCopy("Handyman services", "service", "From R250", "Request this service")
    else -> BoardCopy("Second-hand market", "item", "R150", "Message seller")
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
                title = { Text(copy.title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { TextButton(onClick = { posting = !posting }) { Text(if (posting) "Close" else "Post") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search area or title") }, singleLine = true)
                TextButton(onClick = { viewModel.load(query) }) { Text("Search") }
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
            }
            if (posting) {
                item {
                    OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("${copy.noun.replaceFirstChar { it.uppercase() }} title") })
                    OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth(), label = { Text("Details") }, minLines = 3)
                    OutlinedTextField(price, { price = it }, Modifier.fillMaxWidth(), label = { Text(copy.priceHint) })
                    OutlinedTextField(area, { area = it }, Modifier.fillMaxWidth(), label = { Text("Area") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { picker.launch("image/*") }) { Text(if (photo.isBlank()) "Add photo" else "Photo added") }
                        Button(onClick = {
                            viewModel.publish(title.trim(), details.trim(), price.trim(), area.trim(), photo)
                            posting = false
                            title = ""; details = ""; price = ""; area = ""; photo = ""
                        }, enabled = title.isNotBlank()) { Text("Publish") }
                    }
                }
            }
            if (mine.isNotEmpty()) {
                item { Text("Your listings", fontWeight = FontWeight.Bold) }
                items(mine, key = { "mine-${it.id}" }) { item ->
                    ListingCard(item, "Mark unavailable") { viewModel.close(item.id) }
                }
            }
            item { Text("Available", fontWeight = FontWeight.Bold) }
            if (items.isEmpty()) item { Text("Nothing is listed yet. Be the first to post.") }
            items(items, key = { it.id }) { item ->
                ListingCard(item, "View") { selected = item }
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
private fun ListingCard(item: BoardListing, action: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (item.photoUrl.isNotBlank()) AsyncImage(item.photoUrl, item.title, Modifier.fillMaxWidth().height(140.dp))
            Text(item.title, fontWeight = FontWeight.Bold)
            Text("${item.priceLabel.ifBlank { "Price on request" }} · ${item.area.ifBlank { "Area not set" }}")
            if (item.description.isNotBlank()) Text(item.description, maxLines = 2)
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}
