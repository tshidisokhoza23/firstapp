package com.example.firstapp.jobs.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.jobs.model.Job
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppSecondary
import com.example.firstapp.common.ui.theme.AppTextPrimary
import com.example.firstapp.common.ui.theme.AppTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(job: Job, onBack: () -> Unit, onApply: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSecondary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.verticalGradient(listOf(job.logoColor, AppSecondary)))
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = job.companyName.take(1).uppercase(),
                                color = job.logoColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(
                            text = job.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = job.companyName,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    JobDetailChip(Icons.Default.LocationOn, job.location)
                    JobDetailChip(Icons.Default.Work, job.jobType)
                }
                
                if (job.salaryRange.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    JobDetailChip(Icons.Default.AttachMoney, job.salaryRange)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Description",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = job.description,
                    color = AppTextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 24.sp
                )

                if (job.requirements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Requirements",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    job.requirements.forEach { req ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("•", color = AppAccent, modifier = Modifier.width(16.dp))
                            Text(text = req, color = AppTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onApply,
                    enabled = !job.isApplied,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (job.isApplied) Color.Gray else AppAccent,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (job.isApplied) "Applied" else "Apply Now", 
                        color = if (job.isApplied) Color.White else AppBackground, 
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 16.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun JobDetailChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = AppAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
