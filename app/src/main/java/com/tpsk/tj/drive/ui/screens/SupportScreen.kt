package com.tpsk.tj.drive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.tpsk.tj.common.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    var selectedTopic by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppSecondary)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.Help, null, tint = AppAccent, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("How can we help?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            item {
                Text(
                    "Common Topics",
                    modifier = Modifier.padding(16.dp),
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                SupportTopicItem(Icons.Default.DirectionsCar, "Trip Issues") { selectedTopic = "Trip Issues" }
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SupportTopicItem(Icons.Default.Payment, "Payment and Pricing") { selectedTopic = "Payment and Pricing" }
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SupportTopicItem(Icons.Default.AccountCircle, "Account and Data") { selectedTopic = "Account and Data" }
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                SupportTopicItem(Icons.Default.Shield, "Safety and Security") { selectedTopic = "Safety and Security" }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:support@firstapp.com")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Support Request")
                        }
                        runCatching { context.startActivity(intent) }
                            .onFailure {
                                android.widget.Toast.makeText(context, "No email app is installed", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Contact Support Team", color = AppBackground, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    selectedTopic?.let { topic ->
        TopicDetailDialog(topic = topic, onDismiss = { selectedTopic = null })
    }
}

@Composable
fun SupportTopicItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = AppTextSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, null, tint = AppTextSecondary)
    }
}

@Composable
fun TopicDetailDialog(topic: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = { Text(topic, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Text(
                when (topic) {
                    "Trip Issues" -> "If a driver has not moved, cancel before the PIN is entered and you are not charged a waiting fee. After the trip starts, use Safety to share the trip or call 112. Waiting is free for 3 minutes, then R2 per minute."
                    "Payment and Pricing" -> "Riders pay from the wallet by card or 1Voucher. Cash is not accepted. The fare is R20 plus the vehicle rate per kilometre plus R1.50 per live traffic minute. TJ keeps 20%. Drivers are paid the full 80% every Monday at 06:00."
                    "Account and Data" -> "Open the profile at the top of the menu for Settings. Documents and vehicle details are reviewed before a driver can go online. A rejected document can be uploaded again from Documents."
                    else -> "During a trip, Safety can share your status or open the dialer for 112. The driver cannot start the trip until you give them the PIN."
                },
                color = AppTextSecondary,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = AppAccent)
            }
        }
    )
}
