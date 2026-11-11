package com.tpsk.tj.stokvel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.tpsk.tj.stokvel.data.model.StokvelMember
import com.tpsk.tj.stokvel.ui.viewmodel.RotationViewModel

private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val PrimaryBlue = Color(0xFF2563EB)
private val ActiveGold = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotationCycleScreen(
    group: StokvelGroup,
    members: List<StokvelMember>,
    onBack: () -> Unit,
    viewModel: RotationViewModel = hiltViewModel()
) {
    var showSwapDialog by remember { mutableStateOf(false) }
    var selectedTargetMemberId by remember { mutableStateOf("") }
    val advanceState by viewModel.advanceCycleState.collectAsState()
    val swapConfirmState by viewModel.swapConfirmState.collectAsState()

    val currentRecipient = members.find { it.payoutOrder == group.currentCycleIndex }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Umgalelo Rotation Roster", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shuffleOrder(group.id) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Shuffle Roster", tint = PrimaryBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Recipient Highlight Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current Cycle #${group.currentCycleIndex} Recipient", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentRecipient?.let { "Member #${it.id.takeLast(4)} (Order #${it.payoutOrder})" } ?: "Unassigned",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ActiveGold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Target Pot Amount: R${group.monthlyTargetAmount}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rotation Roster (1..N)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Button(
                    onClick = { viewModel.advanceCycle(group.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Advance Cycle")
                }
            }

            when (advanceState) {
                is Resource.Error -> {
                    Text(
                        text = advanceState?.message ?: "Error advancing cycle",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {}
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(members.sortedBy { it.payoutOrder ?: Int.MAX_VALUE }) { member ->
                    val isActive = member.payoutOrder == group.currentCycleIndex
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) Color(0xFF1E3A8A) else CardBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Order #${member.payoutOrder ?: "-"} • Member ID: ${member.id.takeLast(6)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Role: ${member.role.name} • Score: ${member.reliabilityScore}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.LightGray
                                )
                            }

                            IconButton(onClick = {
                                selectedTargetMemberId = member.id
                                showSwapDialog = true
                            }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Turn", tint = PrimaryBlue)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSwapDialog) {
        AlertDialog(
            onDismissRequest = { showSwapDialog = false },
            title = { Text("Request Turn Swap") },
            text = { Text("Initiate payout order swap with Member ${selectedTargetMemberId.takeLast(6)}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.requestSwap(group.id, requesterMemberId = "mem-A", targetMemberId = selectedTargetMemberId)
                    showSwapDialog = false
                }) {
                    Text("Confirm Request", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwapDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
