package com.example.firstapp.jobs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.jobs.model.Job
import com.example.firstapp.common.ui.theme.AppTextPrimary
import com.example.firstapp.common.ui.theme.AppTextSecondary
import com.example.firstapp.common.util.formatPostedTime

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Verified
import com.example.firstapp.common.ui.theme.AppAccent

import androidx.compose.foundation.clickable

@Composable
fun JobItem(job: Job, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Company Logo
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(job.logoColor),
            contentAlignment = Alignment.Center
        ) {
            if (!job.companyLogoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = job.companyLogoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = job.companyName.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = job.title,
                color = AppTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = job.companyName,
                    color = AppTextPrimary.copy(alpha = 0.9f),
                    fontSize = 15.sp
                )
                if (job.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = "Verified",
                        tint = AppAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            
            Text(
                text = job.location,
                color = AppTextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))
            
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = job.jobType,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (job.postedTime > 0L) {
                Text(
                    text = formatPostedTime(job.postedTime),
                    modifier = Modifier.padding(top = 4.dp),
                    color = AppTextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
