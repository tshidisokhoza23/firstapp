package com.tpsk.tj.stokvel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.stokvel.data.model.ContributionStatus
import com.tpsk.tj.stokvel.data.model.PaymentMethod
import com.tpsk.tj.stokvel.data.model.StokvelContribution
import com.tpsk.tj.stokvel.ui.viewmodel.ContributionViewModel

private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val PrimaryBlue = Color(0xFF2563EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProofOfPaymentScreen(
    groupId: String,
    contributions: List<StokvelContribution>,
    isAdminOrTreasurer: Boolean,
    onBack: () -> Unit,
    viewModel: ContributionViewModel = hiltViewModel()
) {
    var amountText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER) }
    var voucherSerial by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("deposit_slip.jpg") }
    var selectedContribForReview by remember { mutableStateOf<StokvelContribution?>(null) }
    var rejectionReason by remember { mutableStateOf("") }

    val uploadState by viewModel.uploadState.collectAsState()
    val verifyState by viewModel.verifyState.collectAsState()

    val maskedPreview = remember(voucherSerial) {
        if (voucherSerial.isNotBlank()) viewModel.maskVoucherSerial(voucherSerial) else ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proof of Payment (POP)", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            // Upload Form Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Submit Contribution Proof", fontWeight = FontWeight.Bold, color = PrimaryBlue)

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (ZAR R)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Method", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PaymentMethod.values().forEach { method ->
                            val sel = method == selectedMethod
                            FilterChip(
                                selected = sel,
                                onClick = { selectedMethod = method },
                                label = { Text(method.name.replace("_", " "), style = MaterialTheme.typography.bodySmall) }
                            )
                        }
                    }

                    if (selectedMethod == PaymentMethod.ONE_VOUCHER) {
                        OutlinedTextField(
                            value = voucherSerial,
                            onValueChange = { voucherSerial = it },
                            label = { Text("16-Digit 1Voucher Serial PIN") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (maskedPreview.isNotBlank()) {
                            Text(
                                text = "Audit Masked Serial: $maskedPreview",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Green
                            )
                        }
                    }

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Slip Attachment File Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            viewModel.uploadContribution(
                                groupId = groupId,
                                memberId = "mem-current",
                                amount = amt,
                                method = selectedMethod,
                                voucherSerial = voucherSerial.ifBlank { null }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Upload Proof of Payment", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("Contribution Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(contributions) { contrib ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
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
                                Text("R${contrib.amount} • ${contrib.method.name}", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Cycle #${contrib.cycleIndex} • Status: ${contrib.status.name}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                contrib.voucherSerialMasked?.let {
                                    Text("Masked PIN: $it", style = MaterialTheme.typography.bodySmall, color = Color.Cyan)
                                }
                            }

                            if (isAdminOrTreasurer && contrib.status == ContributionStatus.PENDING_VERIFICATION) {
                                Button(
                                    onClick = { selectedContribForReview = contrib },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Verify")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Admin Verification Dialog
    selectedContribForReview?.let { contrib ->
        AlertDialog(
            onDismissRequest = { selectedContribForReview = null },
            title = { Text("Verify Contribution POP") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Review slip for R${contrib.amount} (${contrib.method.name})")
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection Reason (If rejecting)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.verifyContribution(groupId, contrib.id, verifierUserId = "admin-1", action = "approve")
                    selectedContribForReview = null
                }) {
                    Text("Approve", color = Color.Green, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.verifyContribution(groupId, contrib.id, verifierUserId = "admin-1", action = "reject", reason = rejectionReason)
                    selectedContribForReview = null
                }) {
                    Text("Reject", color = Color.Red)
                }
            }
        )
    }
}
