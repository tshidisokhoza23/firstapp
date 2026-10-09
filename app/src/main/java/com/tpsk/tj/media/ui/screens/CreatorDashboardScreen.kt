package com.tpsk.tj.media.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.LocalCurrentUser
import com.tpsk.tj.media.ui.viewmodel.CreatorDashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorDashboardScreen(
    onBack: () -> Unit,
    onCreatePostClick: () -> Unit,
    viewModel: CreatorDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isArtist = uiState.isArtist

    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFF8A2BE2), Color(0xFF00E5FF))
    )

    Scaffold(
        containerColor = Color(0xFF1A1A1A),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Creator Dashboard",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        listOf("7D", "30D", "All").forEach { range ->
                            val isSelected = uiState.selectedTimeRange == range
                            Surface(
                                onClick = { viewModel.selectTimeRange(range) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF242424),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color(0xFF333333))
                            ) {
                                Text(
                                    text = range,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
            )
        },
        floatingActionButton = {
            Button(
                onClick = onCreatePostClick,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .height(48.dp)
                    .background(gradientBrush, RoundedCornerShape(24.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Post", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Top Summary Cards Grid (2x2)
            item {
                if (!uiState.note.isNullOrBlank()) {
                    Text(uiState.note.orEmpty(), color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = "OVERVIEW",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Earnings Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("This Month", color = Color.Gray, fontSize = 12.sp)
                            Text(uiState.earningsThisMonth, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("All time: ${uiState.earningsAllTime}", color = Color(0xFF00E5FF), fontSize = 11.sp)
                        }
                    }

                    // Tips Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Tips Received", color = Color.Gray, fontSize = 12.sp)
                            Text(uiState.tipsTotalAmount, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${uiState.tipsReceivedCount} tips total", color = Color(0xFF8A2BE2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile Views
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Post views", color = Color.Gray, fontSize = 12.sp)
                            Text("${uiState.postViews}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Followers
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("New Followers", color = Color.Gray, fontSize = 12.sp)
                            Text("+${uiState.newFollowers}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Artist Tools Section (if isArtist == true)
            if (uiState.posts.isNotEmpty()) {
                item {
                    Text("YOUR POSTS", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.posts.forEach { post ->
                        Text(
                            text = "${post.views} views · ${post.likes} likes · ${post.text.ifBlank { "Post" }}",
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }
            }

            if (isArtist) {
                item {
                    Text(
                        text = "ARTIST TOOLS & COMMISSIONS",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Commission Requests Inbox", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${uiState.commissionRequests.size} Pending",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (uiState.commissionRequests.isEmpty()) {
                                Text(
                                    "No commission requests. This inbox stays empty until someone sends a real request.",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                            uiState.commissionRequests.forEach { req ->
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(req.clientName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(req.budget, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Text("Deadline: ${req.deadline} • Status: ${req.status}", color = Color.Gray, fontSize = 12.sp)

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.updateCommissionStatus(req.id, "Accepted") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.updateCommissionStatus(req.id, "Declined") },
                                            border = BorderStroke(1.dp, Color.Gray),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Decline", color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 6.dp))
                            }
                        }
                    }
                }

                // Active Commissions Tracker
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Active Commissions Tracker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            if (uiState.activeCommissions.isEmpty()) {
                                Text("No active commissions.", color = Color.Gray, fontSize = 13.sp)
                            }

                            uiState.activeCommissions.forEach { active ->
                                Column {
                                    Text("${active.title} — ${active.clientName}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Current stage: ${active.progressStage}", color = Color(0xFF00E5FF), fontSize = 12.sp)

                                    // Stage Progress Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        listOf("Sketch", "Lineart", "Color", "Done").forEach { stage ->
                                            val isCurrentOrPast = stage == active.progressStage || stage == "Sketch"
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(if (isCurrentOrPast) Color(0xFF00E5FF) else Color(0xFF444444))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Monetization Controls
            item {
                Text(
                    text = "MONETIZATION CONTROLS",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF242424)),
                    border = BorderStroke(1.dp, Color(0xFF333333)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Accept Tips on Posts", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = uiState.acceptTips,
                                onCheckedChange = { viewModel.toggleAcceptTips(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        HorizontalDivider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Accept Commissions", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = uiState.acceptCommissions,
                                onCheckedChange = { viewModel.toggleAcceptCommissions(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { },
                                border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Set Prices", color = Color(0xFF00E5FF), fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { },
                                border = BorderStroke(1.dp, Color.Gray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Payout Settings", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
