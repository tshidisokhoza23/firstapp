package com.example.firstapp.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.Advert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertDetailScreen(advert: Advert, onBack: () -> Unit) {
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
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { 0.7f },
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = Color.White,
                    trackColor = Color.Gray.copy(alpha = 0.5f),
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(advert.avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(advert.advertiserName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(advert.advertiserName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Today, ${advert.time}", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                }
            }

            // Content Area (Image placeholder)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .background(advert.contentColor),
                contentAlignment = Alignment.Center
            ) {
                if (advert.contentText.isNotEmpty()) {
                    Text(
                        text = advert.contentText,
                        color = Color.Black,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(24.dp),
                        lineHeight = 28.sp
                    )
                } else {
                    Text("Ad Content for ${advert.advertiserName}", color = Color.Gray)
                }
            }

            // Bottom Interaction Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Reply", color = Color.White) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFF1F2C34),
                        focusedContainerColor = Color(0xFF1F2C34),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        cursorColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.Sync, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }
    }
}
