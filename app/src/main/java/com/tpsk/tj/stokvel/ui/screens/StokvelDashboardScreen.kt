package com.tpsk.tj.stokvel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.stokvel.data.model.StokvelGroup
import com.tpsk.tj.stokvel.ui.viewmodel.StokvelViewModel

private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val PrimaryBlue = Color(0xFF2563EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StokvelDashboardScreen(
    onBack: () -> Unit,
    onCreateStokvelClick: () -> Unit = {},
    onRotationClick: (StokvelGroup) -> Unit = {},
    onPopClick: (StokvelGroup) -> Unit = {},
    onTenderBoardClick: (StokvelGroup) -> Unit = {},
    viewModel: StokvelViewModel = hiltViewModel()
) {
    val stokvelsState by viewModel.stokvelsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasi Stokvel & Bulk Engine", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateStokvelClick,
                containerColor = PrimaryBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Stokvel", tint = Color.White)
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (stokvelsState) {
                is Resource.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PrimaryBlue)
                }
                is Resource.Success -> {
                    val stokvels = stokvelsState.data ?: emptyList()
                    if (stokvels.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Stokvels yet",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create or join a Stokvel to start bulk reverse-bidding.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onCreateStokvelClick,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Stokvel Group", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(stokvels) { group ->
                                StokvelGroupCard(
                                    group = group,
                                    onRotationClick = { onRotationClick(group) },
                                    onPopClick = { onPopClick(group) },
                                    onTenderBoardClick = { onTenderBoardClick(group) }
                                )
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Text(
                        text = stokvelsState.message ?: "An error occurred",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun StokvelGroupCard(
    group: StokvelGroup,
    onRotationClick: () -> Unit,
    onPopClick: () -> Unit,
    onTenderBoardClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    color = PrimaryBlue.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = group.type.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Target: R${group.monthlyTargetAmount} / ${group.cycleInterval} • Cycle #${group.currentCycleIndex}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )

            group.bankName?.let { bank ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Bank: $bank (${group.accountNumberHint ?: ""})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onRotationClick) {
                    Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Rotation Roster", tint = PrimaryBlue)
                }
                IconButton(onClick = onPopClick) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Proof of Payment", tint = PrimaryBlue)
                }
                IconButton(onClick = onTenderBoardClick) {
                    Icon(Icons.Default.Storefront, contentDescription = "Supplier Tender Board", tint = PrimaryBlue)
                }
            }
        }
    }
}
