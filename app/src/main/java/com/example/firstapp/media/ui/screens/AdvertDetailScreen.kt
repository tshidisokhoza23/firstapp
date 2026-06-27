package com.example.firstapp.media.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.media.model.Advert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertDetailScreen(advert: Advert, onBack: () -> Unit) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Story Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { 0.75f },
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
            }

            // Top Profile Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(advert.avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (!advert.avatarUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = advert.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(advert.advertiserName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(advert.advertiserName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Sponsored", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(com.example.firstapp.common.ui.theme.AppSecondary)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share advert", color = Color.White) },
                            onClick = {
                                showMenu = false
                                val shareText = "${advert.advertiserName}: ${advert.targetUrl}"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share advert"))
                            }
                        )
                    }
                }
            }

            // Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .background(com.example.firstapp.common.ui.theme.AppBackground),
                contentAlignment = Alignment.Center
            ) {
                if (!advert.contentUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = advert.contentUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                }
                
                if (advert.contentText.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = advert.contentText,
                            color = Color.White,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(20.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 26.sp
                        )
                    }
                }
            }

            // Bottom Interaction Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(advert.targetUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.firstapp.common.ui.theme.AppAccent),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(advert.actionText ?: "Learn More", color = com.example.firstapp.common.ui.theme.AppBackground, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(onClick = {}) { Icon(Icons.Default.Sync, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(28.dp)) }
                    IconButton(onClick = {}) { Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
            }
        }
    }
}
