package com.tpsk.tj.chat.jobs.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.chat.jobs.util.CustomTabsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailScreen(
    job: Job,
    onBack: () -> Unit,
    onApply: () -> Unit,
    onExternalApplyClick: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onReportJob: (String, String) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val isAppliedInternal = job.isApplied || job.hasAppliedInternal

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
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report Job Listing", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Report,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                }
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
            com.tpsk.tj.common.ui.components.BannerAdView(
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                if (job.isExternal) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Partner Listing • Listed via ${job.sourcePartner ?: "External Source"}",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

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

                if (job.isExternal) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Affiliate Disclosure: Listing provided via ${job.sourcePartner ?: "External Source"}. Tapping 'Apply' redirects to employer website.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = {
                        if (job.isExternal) {
                            onExternalApplyClick(job.id)
                            val applyUrl = job.trackingApplyUrl.takeIf { !it.isNullOrBlank() }
                                ?: "${com.tpsk.tj.common.data.network.NetworkConstants.BASE_URL}api/v1/jobs/${job.id}/apply"
                            CustomTabsHelper.launchUrl(context, applyUrl)
                        } else {
                            onApply()
                        }
                    },
                    enabled = job.isExternal || !isAppliedInternal,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (job.isExternal) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        contentColor = if (job.isExternal) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (job.isExternal) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (job.hasClickedExternal) "Open Again (${job.sourcePartner ?: "Partner Site"})" else "Apply on ${job.sourcePartner ?: "Partner Site"}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        } else {
                            Text(
                                text = if (isAppliedInternal) "Applied" else "Apply Now",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showReportDialog) {
        var selectedReason by remember { mutableStateOf("Fraudulent or misleading") }
        val reasons = listOf("Fraudulent or misleading", "Discriminatory content", "Spam or scam", "Inappropriate content", "Other")

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Job Listing", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Why are you reporting this job listing?", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = (selectedReason == reason),
                                onClick = { selectedReason = reason }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(reason, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReportJob(job.id, selectedReason)
                        android.widget.Toast.makeText(context, "Thank you. Report submitted for review.", android.widget.Toast.LENGTH_LONG).show()
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Report", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun JobDetailChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
