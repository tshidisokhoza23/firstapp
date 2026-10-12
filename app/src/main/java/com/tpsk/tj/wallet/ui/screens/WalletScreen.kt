package com.tpsk.tj.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallMade
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
import com.tpsk.tj.chat.ui.viewmodel.AdvertsViewModel
import com.tpsk.tj.wallet.ui.CreatorWalletSection

data class WalletTxItem(
    val id: String,
    val title: String,
    val counterparty: String,
    val timestamp: String,
    val amountFormatted: String,
    val isCredit: Boolean,
    val status: String = "Completed"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    advertsViewModel: AdvertsViewModel
) {
    val points by advertsViewModel.points.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Received", "Sent", "Payouts")

    var toastMessage by remember { mutableStateOf<String?>(null) }

    val gradientBackground = Brush.linearGradient(
        colors = listOf(Color(0xFF3F51B5), Color(0xFF8E24AA), Color(0xFF1E88E5))
    )

    val sampleTransactions = remember {
        listOf(
            WalletTxItem("1", "Reward Bonus", "TJ Rewards", "Today", "+10.0 pts", true),
            WalletTxItem("2", "Post Tip", "Creator Tip", "Yesterday", "-5.0 pts", false)
        )
    }

    val filteredTxs = remember(selectedFilter, sampleTransactions) {
        sampleTransactions.filter { tx ->
            when (selectedFilter) {
                "Received" -> tx.isCredit
                "Sent" -> !tx.isCredit
                "Payouts" -> tx.title.contains("Payout", ignoreCase = true)
                else -> true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Balance Card View matching iOS
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .background(gradientBackground)
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.Yellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ad points",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Not cash",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = java.lang.String.format(java.util.Locale.US, "%.2f", points),
                                    color = Color.White,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "points",
                                    color = Color(0xFF00E5FF),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }

                            Text(
                                text = "Ad points cannot be tipped or withdrawn. Creator coins are a separate purchase.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Quick Actions Row matching iOS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WalletActionButton(
                        title = "Coins",
                        icon = Icons.Default.CardGiftcard,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f),
                        onClick = { toastMessage = "Buy coin packs below. Coins can only tip posts inside TJ." }
                    )
                    WalletActionButton(
                        title = "Bank",
                        icon = Icons.Default.AccountBalance,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f),
                        onClick = { toastMessage = "Verify bank details in the creator wallet section below." }
                    )
                    WalletActionButton(
                        title = "Hold",
                        icon = Icons.Default.Schedule,
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f),
                        onClick = { toastMessage = "Creator earnings stay on hold for 14 days and are taxable income." }
                    )
                    WalletActionButton(
                        title = "Payout",
                        icon = Icons.Default.Payments,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f),
                        onClick = { toastMessage = "Payouts require a verified South African bank account." }
                    )
                }
            }

            // Creator Coins Section
            item { CreatorWalletSection() }

            // Linked Payout Methods Section matching iOS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "LINKED PAYOUT METHODS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "No payout method on file",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Primary",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Transaction History Section matching iOS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "TRANSACTION HISTORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )

                    // Filter Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filters) { filter ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                onClick = { selectedFilter = filter },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = filter,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            if (filteredTxs.isEmpty()) {
                                Text(
                                    text = "No transactions found",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            } else {
                                filteredTxs.forEachIndexed { index, tx ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(if (tx.isCredit) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFF44336).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (tx.isCredit) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                                                    contentDescription = null,
                                                    tint = if (tx.isCredit) Color(0xFF4CAF50) else Color(0xFFF44336),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = tx.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${tx.counterparty} • ${tx.timestamp}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = tx.amountFormatted,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (tx.isCredit) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = tx.status,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50)
                                            )
                                        }
                                    }
                                    if (index < filteredTxs.size - 1) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    toastMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { toastMessage = null },
            title = { Text("Wallet Notice") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { toastMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun WalletActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
