package com.tpsk.tj.mingle.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.chat.ui.viewmodel.ChatViewModel
import com.tpsk.tj.common.model.ConversationType
import com.tpsk.tj.common.ui.components.BannerAdView
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MingleChatsScreen(
    navController: NavController,
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    mingleViewModel: com.tpsk.tj.mingle.ui.viewmodel.MingleViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Play Store Compliance & Screen Privacy: Enforce FLAG_SECURE for Secret Chats
    DisposableEffect(Unit) {
        val activity = context as? android.app.Activity
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val mingleUi by mingleViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        mingleViewModel.loadIntros()
    }
    val mingleChatsState by chatViewModel.mingleChats.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val mingleChats = remember(mingleChatsState, searchQuery) {
        val allChats = (mingleChatsState.data ?: emptyList()).filter {
            it.type == ConversationType.MINGLE || it.isMingle || it.id.startsWith("mingle_")
        }

        if (searchQuery.isBlank()) {
            allChats
        } else {
            allChats.filter {
                it.senderName.contains(searchQuery, ignoreCase = true) ||
                it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mingle Secret Chats",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Private secret chats with your mutual matches",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE91E63).copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Encrypted",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE91E63)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            BannerAdView(modifier = Modifier.navigationBarsPadding())
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Mingle secret chats...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (searchQuery.isBlank() && mingleChats.isNotEmpty()) {
                Text(
                    text = "Matches",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(mingleChats, key = { "match-${it.id}" }) { chat ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .clickable {
                                    navController.navigate(
                                        Screen.Chat.createRoute(chat.id, type = "mingle", displayName = chat.senderName)
                                    )
                                }
                        ) {
                            val photo = com.tpsk.tj.common.util.MediaUtils.formatUrl(chat.avatarUrl)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE91E63)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (photo != null) {
                                    AsyncImage(
                                        model = photo,
                                        contentDescription = chat.senderName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = chat.senderName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = chat.senderName.ifBlank { "Match" },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            if (searchQuery.isBlank() && mingleUi.intros.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { navController.navigate(Screen.MingleMessageRequests.route) },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MailOutline, contentDescription = null, tint = Color(0xFFE91E63))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Message Requests (${mingleUi.intros.size})",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            if (mingleChats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching secret chats" else "No Mingle Secret Chats Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching for a different name." else "Match with people on Mingle to start private, secret chats.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    navController.popBackStack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Explore Mingle Candidates", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 80.dp // Ensure last conversation item is not obscured by banner ad
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(mingleChats, key = { it.id }) { chat ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(Screen.Chat.createRoute(chat.id, type = "mingle", displayName = chat.senderName))
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar with initial and Mingle badge
                                Box(
                                    modifier = Modifier.size(52.dp),
                                    contentAlignment = Alignment.BottomEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(Color(0xFFE91E63)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = chat.senderName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2196F3)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = chat.senderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Secret Chat",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFE91E63)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = chat.lastMessage,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
