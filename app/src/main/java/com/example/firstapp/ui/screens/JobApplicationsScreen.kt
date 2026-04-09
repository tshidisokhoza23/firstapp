package com.example.firstapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.model.JobApplication
import com.example.firstapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicationsScreen(
    applications: List<JobApplication>,
    onBack: () -> Unit,
    onViewJobDetails: (Int) -> Unit,
    onWithdrawApplication: (Int) -> Unit,
    onContactRecruiter: (JobApplication) -> Unit,
    onSetReminder: (JobApplication) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application History", fontWeight = FontWeight.Bold) },
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
        if (applications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No applications yet", color = AppTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(applications) { application ->
                    ApplicationItem(
                        application = application,
                        onViewJobDetails = { onViewJobDetails(application.jobId) },
                        onWithdrawApplication = { onWithdrawApplication(application.id) },
                        onContactRecruiter = { onContactRecruiter(application) },
                        onSetReminder = { onSetReminder(application) }
                    )
                }
            }
        }
    }
}

@Composable
fun ApplicationItem(
    application: JobApplication,
    onViewJobDetails: () -> Unit,
    onWithdrawApplication: () -> Unit,
    onContactRecruiter: () -> Unit,
    onSetReminder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppSecondary.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(application.logoColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Work, contentDescription = null, tint = Color.White)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = application.jobTitle,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = application.companyName,
                    color = AppTextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Applied on ${application.appliedDate}",
                    color = AppTextSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            StatusBadge(application.status.label, application.status.color)
            
            ApplicationActions(
                onViewJobDetails = onViewJobDetails,
                onWithdrawApplication = onWithdrawApplication,
                onContactRecruiter = onContactRecruiter,
                onSetReminder = onSetReminder
            )
        }
    }
}

@Composable
fun ApplicationActions(
    onViewJobDetails: () -> Unit,
    onWithdrawApplication: () -> Unit,
    onContactRecruiter: () -> Unit,
    onSetReminder: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More",
                tint = AppTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(AppSecondary)
        ) {
            DropdownMenuItem(
                text = { Text("View Job Details", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = AppAccent) },
                onClick = { 
                    showMenu = false
                    onViewJobDetails()
                }
            )
            DropdownMenuItem(
                text = { Text("Withdraw Application", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red) },
                onClick = { 
                    showMenu = false
                    onWithdrawApplication()
                }
            )
            DropdownMenuItem(
                text = { Text("Contact Recruiter", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = AppAccent) },
                onClick = { 
                    showMenu = false
                    onContactRecruiter()
                }
            )
            DropdownMenuItem(
                text = { Text("Set Reminder", color = Color.White) },
                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = AppAccent) },
                onClick = { 
                    showMenu = false
                    onSetReminder()
                }
            )
        }
    }
}

@Composable
fun StatusBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
