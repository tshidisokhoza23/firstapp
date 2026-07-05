package com.example.firstapp.media.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.media.model.Advert
import com.example.firstapp.common.ui.theme.*

@Composable
fun AdvertItem(advert: Advert, onClick: () -> Unit = {}) {
    val displayUrl = if (advert.targetUrl.isNotBlank()) {
        try {
            advert.targetUrl.replace("https://", "").replace("http://", "").split("/").first()
        } catch (e: Exception) { "" }
    } else ""

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Advertiser Avatar
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Color(advert.avatarColor)
            ) {
                if (!advert.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = advert.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Header: Name, Ad Badge, More
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = advert.advertiserName,
                        color = AppTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "Ad",
                        color = AppTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = AppTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Content Text
                Text(
                    text = advert.contentText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = AppTextPrimary,
                        lineHeight = 20.sp
                    )
                )

                // Large Media Image (if available)
                if (!advert.contentUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = advert.contentUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }

                // Action Footer
                if (displayUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "from $displayUrl",
                            color = AppTextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = advert.actionText,
                            color = AppAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { /* Handle click */ }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = AppAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                // Mock stats to look like a tweet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = AppTextSecondary
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ChatBubbleOutline, null, tint = color, modifier = Modifier.size(18.dp))
                        if (advert.impressions.isNotEmpty()) {
                            Spacer(Modifier.width(4.dp))
                            Text("27", color = color, fontSize = 12.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, null, tint = color, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("46", color = color, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FavoriteBorder, null, tint = color, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("868", color = color, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.BarChart, null, tint = color, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("4,9M", color = color, fontSize = 12.sp)
                    }
                    Icon(Icons.Outlined.IosShare, null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
