package com.example.firstapp.media.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.media.model.Advert
import com.example.firstapp.common.ui.theme.AppTextPrimary
import com.example.firstapp.common.ui.theme.AppTextSecondary

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.firstapp.common.ui.theme.AppAccent

@Composable
fun AdvertItem(advert: Advert) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Avatar with Accent Ring
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(2.5.dp, AppAccent, CircleShape)
                .padding(3.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(advert.avatarColor),
                contentAlignment = Alignment.Center
            ) {
                if (!advert.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = advert.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = advert.advertiserName.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = advert.advertiserName,
                color = AppTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = com.example.firstapp.common.util.formatPostedTime(advert.time),
                color = AppTextSecondary,
                fontSize = 14.sp
            )
        }
        
        Surface(
            color = AppAccent.copy(alpha = 0.1f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AppAccent.copy(alpha = 0.2f))
        ) {
            Text(
                text = "Ad",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                color = AppAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
