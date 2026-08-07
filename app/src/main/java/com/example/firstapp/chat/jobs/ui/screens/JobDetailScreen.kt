package com.example.firstapp.chat.jobs.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.chat.jobs.model.Job
import com.example.firstapp.common.util.formatPostedTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(job: Job, onBack: () -> Unit, onApply: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job Details", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back", 
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            // Sticky Banner Ad at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                com.example.firstapp.media.ui.components.BannerAdView(
                    adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test ID
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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
                    .background(Brush.verticalGradient(listOf(job.logoColor, MaterialTheme.colorScheme.surfaceVariant)))
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
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
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = job.companyName,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = job.description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 24.sp
                )

                if (job.requirements.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Requirements",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    job.requirements.forEach { req ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("•", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(16.dp))
                            Text(text = req, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onApply,
                    enabled = !job.isApplied,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (job.isApplied) "Applied" else "Apply Now", 
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
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
