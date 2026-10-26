package com.tpsk.tj.chat.jobs.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel
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
import com.tpsk.tj.chat.jobs.model.JobApplication
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.common.util.formatWhatsAppTime
import com.tpsk.tj.chat.jobs.ui.viewmodel.JobApplicationsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobApplicationsScreen(
    onBack: () -> Unit,
    onViewJobDetails: (String) -> Unit,
    onContactRecruiter: (String) -> Unit, // Pass conversationId if available
    onSetReminder: (JobApplication) -> Unit,
    viewModel: JobApplicationsViewModel = hiltViewModel(),
) {
    val applicationsResource by viewModel.applications.collectAsState()
    val applications = applicationsResource.data ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application History", fontWeight = FontWeight.Bold) },
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
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (applications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No applications yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        onWithdrawApplication = { viewModel.withdrawApplication(application.id) },
                        onContactRecruiter = {
                            onContactRecruiter(application.conversationId.orEmpty())
                        },
                    ) { onSetReminder(application) }
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
    onSetReminder: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${application.companyName} • ${application.location}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    text = "Applied on ${formatWhatsAppTime(application.appliedDate)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
    var showMenu by remember { mutableStateOf(value = false) }
    
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            DropdownMenuItem(
                text = { Text("View Job Details", color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { 
                    showMenu = false
                    onViewJobDetails()
                }
            )
            DropdownMenuItem(
                text = { Text("Withdraw Application", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = { 
                    showMenu = false
                    onWithdrawApplication()
                }
            )
            DropdownMenuItem(
                text = { Text("Contact Recruiter", color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                onClick = { 
                    showMenu = false
                    onContactRecruiter()
                }
            )
            DropdownMenuItem(
                text = { Text("Set Reminder", color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
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
