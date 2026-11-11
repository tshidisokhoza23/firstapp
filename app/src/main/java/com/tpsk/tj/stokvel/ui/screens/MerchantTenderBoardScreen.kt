package com.tpsk.tj.stokvel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.stokvel.data.model.RfqItem
import com.tpsk.tj.stokvel.data.model.StokvelRfq
import com.tpsk.tj.stokvel.data.model.SupplierBid
import com.tpsk.tj.stokvel.ui.viewmodel.RfqViewModel

private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val PrimaryBlue = Color(0xFF2563EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantTenderBoardScreen(
    groupId: String,
    rfq: StokvelRfq?,
    onBack: () -> Unit,
    viewModel: RfqViewModel = hiltViewModel()
) {
    var showCreateRfqDialog by remember { mutableStateOf(false) }
    var regionText by remember { mutableStateOf("Tembisa") }
    var itemName by remember { mutableStateOf("10kg Maize Meal") }
    var itemQty by remember { mutableStateOf("40") }

    val bidsState by viewModel.bidsState.collectAsState()
    val acceptBidState by viewModel.acceptBidState.collectAsState()

    LaunchedEffect(rfq) {
        rfq?.let { viewModel.fetchBids(groupId, it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Reverse-Bidding Tender Board", fontWeight = FontWeight.Bold, color = Color.White) },
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
                onClick = { showCreateRfqDialog = true },
                containerColor = PrimaryBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create RFQ", tint = Color.White)
            }
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
            // RFQ Header Card
            rfq?.let { activeRfq ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RFQ #${activeRfq.id.takeLast(6)}", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Text("Status: ${activeRfq.status.name}", style = MaterialTheme.typography.bodySmall, color = Color.Yellow)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Region: ${activeRfq.targetRegion}", color = Color.White)
                        Text("Manifest: ${activeRfq.itemsManifest.joinToString { "${it.quantity}x ${it.name}" }}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    }
                }
            } ?: Text("No active RFQ tender published yet.", color = Color.Gray)

            // Winning Bid Non-Custodial Fulfillment Card
            acceptBidState?.let { res ->
                if (res is Resource.Success && res.data != null) {
                    val fulfillment = res.data
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tender Awarded! (Non-Custodial Direct Payment)", fontWeight = FontWeight.Bold, color = Color(0xFF6EE7B7))
                            Text("Winning Price: R${fulfillment.bid.totalPrice}", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Merchant Direct Bank Details: ${fulfillment.merchantBankDetails}", color = Color.White)
                            Text("Proforma Invoice URL: ${fulfillment.proformaInvoiceUrl}", style = MaterialTheme.typography.bodySmall, color = Color.Cyan)
                            Text("Instructions: ${fulfillment.instructions}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                        }
                    }
                }
            }

            Text("Supplier Bids Received", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)

            when (bidsState) {
                is Resource.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                is Resource.Success -> {
                    val bids = bidsState.data ?: emptyList()
                    if (bids.isEmpty()) {
                        Text("No supplier quotes submitted yet.", color = Color.Gray)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(bids) { bid ->
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
                                            Text("Quote: R${bid.totalPrice}", fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("Fulfilment: ${bid.fulfilmentType} • Status: ${bid.status.name}", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                                            Text("Merchant ID: ${bid.merchantId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }

                                        if (bid.status.name == "SUBMITTED" && rfq != null) {
                                            Button(
                                                onClick = { viewModel.acceptBid(groupId, rfq.id, bid.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                            ) {
                                                Text("Accept Tender")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is Resource.Error -> Text("Error loading bids", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showCreateRfqDialog) {
        AlertDialog(
            onDismissRequest = { showCreateRfqDialog = false },
            title = { Text("Publish Bulk Tender RFQ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = regionText, onValueChange = { regionText = it }, label = { Text("Target Region") })
                    OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item Name") })
                    OutlinedTextField(value = itemQty, onValueChange = { itemQty = it }, label = { Text("Quantity") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val qty = itemQty.toIntOrNull() ?: 1
                    viewModel.createRfq(
                        groupId = groupId,
                        targetRegion = regionText,
                        itemsManifest = listOf(RfqItem(name = itemName, quantity = qty, unit = "units"))
                    )
                    showCreateRfqDialog = false
                }) {
                    Text("Publish RFQ", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateRfqDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
