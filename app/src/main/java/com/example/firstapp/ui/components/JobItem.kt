package com.example.firstapp.ui.components

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
import com.example.firstapp.model.Job
import com.example.firstapp.ui.theme.AppTextPrimary
import com.example.firstapp.ui.theme.AppTextSecondary
import com.example.firstapp.util.formatPostedTime

@Composable
fun JobItem(job: Job) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Company Logo Placeholder
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(job.logoColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = job.companyName.take(1),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = job.title,
                    color = AppTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
            }
            
            if (job.isVerified) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Verified",
                    tint = Color(0xFF5F6368),
                    modifier = Modifier.size(18.dp).padding(vertical = 2.dp)
                )
            }

            Text(
                text = job.companyName,
                color = AppTextPrimary,
                fontSize = 16.sp
            )
            
            Text(
                text = job.location,
                color = AppTextSecondary,
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = listOf(job.jobType, job.salaryRange).filter { it.isNotBlank() }.joinToString(" • "),
                color = AppTextSecondary,
                fontSize = 13.sp
            )

            if (job.postedTime.isNotBlank()) {
                Text(
                    text = formatPostedTime(job.postedTime),
                    color = AppTextSecondary.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
