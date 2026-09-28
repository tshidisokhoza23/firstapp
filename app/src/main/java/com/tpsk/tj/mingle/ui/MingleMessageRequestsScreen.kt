package com.tpsk.tj.mingle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.tpsk.tj.common.util.MediaUtils
import com.tpsk.tj.mingle.data.api.MingleIntroDto
import com.tpsk.tj.mingle.ui.viewmodel.MingleViewModel
import com.tpsk.tj.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MingleMessageRequestsScreen(
    navController: NavController,
    onBack: () -> Unit,
    mingleViewModel: MingleViewModel = hiltViewModel()
) {
    val uiState by mingleViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        mingleViewModel.loadIntros()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Message Requests", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.intros.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No message requests",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When someone sends you a Mingle intro, it will wait here.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.intros, key = { it.id ?: it.sender_id ?: it.message.orEmpty() }) { intro ->
                    MessageRequestCard(
                        intro = intro,
                        onAccept = {
                            mingleViewModel.respondIntro(intro, "accept") { conversationId ->
                                navController.navigate(
                                    Screen.Chat.createRoute(
                                        conversationId,
                                        type = "mingle",
                                        displayName = intro.sender_name ?: "Mingle"
                                    )
                                ) {
                                    popUpTo(Screen.MingleMessageRequests.route) { inclusive = true }
                                }
                            }
                        },
                        onDecline = {
                            mingleViewModel.respondIntro(intro, "decline") {}
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageRequestCard(
    intro: MingleIntroDto,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val name = intro.sender_name?.takeIf { it.isNotBlank() } ?: "Someone"
    val details = buildList {
        if (intro.sender_age > 0) add("${intro.sender_age}")
        intro.sender_location?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.joinToString(" · ")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val photo = MediaUtils.formatUrl(intro.sender_avatar)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE91E63)),
                    contentAlignment = Alignment.Center
                ) {
                    if (photo != null) {
                        AsyncImage(
                            model = photo,
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (details.isNotBlank()) {
                        Text(details, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = intro.message.orEmpty().take(150),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                ) {
                    Text("Accept")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDecline) {
                    Text("Decline")
                }
            }
        }
    }
}
