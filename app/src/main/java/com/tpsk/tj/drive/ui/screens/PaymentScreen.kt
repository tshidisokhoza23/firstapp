package com.tpsk.tj.drive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.util.Locale
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.drive.ui.viewmodel.DriveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(onBack: () -> Unit, viewModel: DriveViewModel, forDriver: Boolean = false, section: String = if (forDriver) "earnings" else "wallet", onOpenPayout: () -> Unit = {}) {
    BackHandler { onBack() }
    var voucherPin by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var bankMenu by remember { mutableStateOf(false) }
    var selectedBank by remember { mutableStateOf<com.tpsk.tj.drive.data.api.PayoutBank?>(null) }
    val balance by viewModel.walletBalance
    val payout by viewModel.availablePayout
    val transactions = viewModel.transactions
    val stats by viewModel.driverStats.collectAsState()
    val trips by viewModel.trips.collectAsState()
    LaunchedEffect(section) {
        viewModel.refreshPayments()
        if (section == "earnings") viewModel.fetchTrips()
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.confirmPendingTopUp()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (section) {
                            "payout" -> "Payout methods"
                            "earnings" -> "Earnings"
                            else -> "Payment"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
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
                Surface(
                    color = AppSecondary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        val shownAmount = when (section) {
                            "earnings" -> stats.weeklyEarnings
                            "payout" -> payout
                            else -> balance
                        }
                        Text(
                            when (section) {
                                "earnings" -> "Your 80% this week"
                                "payout" -> "Ready for Monday"
                                else -> "Wallet balance"
                            },
                            color = AppTextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "R ${String.format(Locale.US, "%.2f", shownAmount)}",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            when (section) {
                                "earnings" -> "Each completed trip below is your share after TJ's 20%. This is not the bank payout screen."
                                "payout" -> "The full available amount is sent to the bank below every Monday at 06:00. A ride still in progress waits until it finishes."
                                else -> "Card or 1Voucher only. Cash is not accepted. The fare is held when you request."
                            },
                            color = AppTextSecondary,
                            fontSize = 13.sp
                        )
                        if (section == "payout") {
                            val account = viewModel.payoutAccount.value
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (account.ready) "Next payout ${account.next_payout_label}. ${account.bank_name} •••• ${account.account_last4}"
                                else "Add a bank account so Monday's payout can be sent.",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            if (account.last_status.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Last payout ${account.last_status} R${String.format(Locale.US, "%.2f", account.last_amount)}",
                                    color = AppTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            return@Column
                        }
                        if (section == "earnings") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onOpenPayout,
                                colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Open payout methods", color = AppBackground, fontWeight = FontWeight.Bold)
                            }
                            return@Column
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(50.0, 100.0, 200.0, 500.0).forEach { amount ->
                                Button(
                                    onClick = {
                                        viewModel.startCardTopUp(amount) { url ->
                                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Text("R${amount.toInt()}", color = AppBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (section == "wallet") item {
                Text(
                    "Payment Methods",
                    modifier = Modifier.padding(16.dp),
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (section == "wallet") item {
                PaymentMethodItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "TJ Wallet",
                    subtitle = "Rides are paid from this balance",
                    isSelected = true
                )
            }

            if (section == "wallet") item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("1Voucher", color = AppTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = voucherPin,
                        onValueChange = { voucherPin = it.filter { ch -> ch.isDigit() }.take(20) },
                        placeholder = { Text("Enter PIN", color = AppTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.redeemVoucher(voucherPin)
                            voucherPin = ""
                        },
                        enabled = voucherPin.length >= 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add voucher to wallet", color = AppBackground, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Change from a voucher stays in your wallet for the next ride.", color = AppTextSecondary, fontSize = 12.sp)
                }
            }

            if (section == "earnings") item {
                Text(
                    "Completed trips",
                    modifier = Modifier.padding(16.dp),
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (section == "earnings") {
                val completed = trips.data.orEmpty().filter { it.driverEarning > 0 || it.status.equals("Completed", true) }
                if (completed.isEmpty()) {
                    item {
                        Text("No completed trips yet.", modifier = Modifier.padding(horizontal = 16.dp), color = AppTextSecondary)
                    }
                }
                items(completed) { trip ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(trip.to.ifBlank { trip.date }, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            "${trip.date} • ${trip.distance.ifBlank { trip.from }} • you get R${String.format(Locale.US, "%.2f", trip.driverEarning)}",
                            color = AppTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (section == "payout") item {
                val banks = viewModel.payoutBanks.value
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("Bank account", color = AppTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        placeholder = { Text("Name on the account", color = AppTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(expanded = bankMenu, onExpandedChange = { bankMenu = it }) {
                        OutlinedTextField(
                            value = selectedBank?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Bank", color = AppTextSecondary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankMenu) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = bankMenu, onDismissRequest = { bankMenu = false }) {
                            banks.forEach { bank ->
                                DropdownMenuItem(
                                    text = { Text(bank.name) },
                                    onClick = {
                                        selectedBank = bank
                                        bankMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it.filter { ch -> ch.isDigit() }.take(17) },
                        placeholder = { Text("Account number", color = AppTextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val bank = selectedBank ?: return@Button
                            viewModel.savePayoutAccount(accountName, accountNumber, bank.code, bank.name) { message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = accountName.length >= 3 && accountNumber.length >= 6 && selectedBank != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save bank for Monday payout", color = AppBackground, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "The full available amount goes out every Monday at 06:00. Amounts under R1 stay until the next Monday.",
                        color = AppTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (section == "wallet") item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Recent Transactions",
                    modifier = Modifier.padding(16.dp),
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (section == "wallet" && transactions.isEmpty()) {
                item {
                    Text(
                        "No payments yet",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = AppTextSecondary
                    )
                }
            }
            if (section == "wallet") items(transactions) { tx ->
                TransactionItem(tx)
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

}

@Composable
fun PaymentMethodItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) AppSecondary.copy(alpha = 0.5f) else Color.Transparent)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = AppTextSecondary, fontSize = 13.sp)
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AppAccent)
        }
    }
}

@Composable
fun TransactionItem(tx: com.tpsk.tj.drive.model.Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (tx.isDebit) Color.White.copy(0.05f) else AppAccent.copy(0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (tx.isDebit) Icons.Default.DirectionsCar else Icons.Default.Add,
                contentDescription = null,
                tint = if (tx.isDebit) Color.White else AppAccent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(tx.date, color = AppTextSecondary, fontSize = 12.sp)
        }
        Text(
            (if (tx.isDebit) "- " else "+ ") + "R${String.format(Locale.getDefault(), "%.2f", Math.abs(tx.amount))}",
            color = if (tx.isDebit) Color.White else AppAccent,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
    }
}
