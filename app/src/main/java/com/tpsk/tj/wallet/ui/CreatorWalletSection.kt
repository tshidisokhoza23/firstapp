package com.tpsk.tj.wallet.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.tpsk.tj.wallet.data.CreatorWalletRepository
import com.tpsk.tj.wallet.data.PlayCoinBilling
import com.tpsk.tj.wallet.data.api.CreatorEarningsDto
import com.tpsk.tj.wallet.data.api.CreatorWalletDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

const val CreatorMoneyNotice =
    "Purchased coins can only be used to tip inside TJ. They are not a bank balance and cannot be withdrawn. Creator payouts are rand earnings, not a coin cash-out. They stay on hold for 14 days, require a verified South African bank account, and are the creator's taxable income. A payout is not paid until the bank transfer completes."

@HiltViewModel
class CreatorWalletViewModel @Inject constructor(
    private val repository: CreatorWalletRepository,
    private val billing: PlayCoinBilling
) : ViewModel() {
    private val _wallet = MutableStateFlow<CreatorWalletDto?>(null)
    val wallet: StateFlow<CreatorWalletDto?> = _wallet
    private val _earnings = MutableStateFlow<CreatorEarningsDto?>(null)
    val earnings: StateFlow<CreatorEarningsDto?> = _earnings
    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                recover()
                _wallet.value = repository.balance()
                _earnings.value = repository.earnings()
                _products.value = billing.products()
            }.onFailure { _message.value = it.message }
        }
    }

    fun buy(activity: Activity, product: ProductDetails) {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                val purchase = billing.buy(activity, product)
                val productId = purchase.products.firstOrNull() ?: product.productId
                _wallet.value = repository.verifyAndroidPurchase(productId, purchase.purchaseToken)
                billing.consume(purchase.purchaseToken)
                _earnings.value = repository.earnings()
                _message.value = "Coins added. They can only be used to tip."
            }.onFailure { _message.value = it.message }
            _busy.value = false
        }
    }

    fun tip(postId: String, coins: Int, key: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                val result = repository.tip(postId, coins, key)
                _wallet.value = _wallet.value?.copy(purchasedCoins = result.purchasedCoins)
                onDone("Tip sent. The creator's earnings stay on hold for 14 days.")
            }.onFailure { onDone(it.message ?: "Tip rejected") }
            _busy.value = false
        }
    }

    fun saveBank(name: String, account: String, bankCode: String) {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                val saved = repository.saveBank(name.trim(), account.trim(), bankCode.trim())
                _message.value = "Bank verified for ${saved.resolvedAccountName}. Only the last 4 digits are kept."
            }.onFailure { _message.value = it.message }
            _busy.value = false
        }
    }

    fun requestPayout() {
        viewModelScope.launch {
            _busy.value = true
            runCatching {
                val payout = repository.requestPayout(UUID.randomUUID().toString())
                _message.value = "Payout ${payout.status}. It is not paid until the bank transfer completes."
                _earnings.value = repository.earnings()
            }.onFailure { _message.value = it.message }
            _busy.value = false
        }
    }

    private suspend fun recover() {
        billing.unconsumed().forEach { purchase ->
            val productId = purchase.products.firstOrNull() ?: return@forEach
            runCatching {
                repository.verifyAndroidPurchase(productId, purchase.purchaseToken)
                billing.consume(purchase.purchaseToken)
            }
        }
    }
}

@Composable
fun CreatorWalletSection(viewModel: CreatorWalletViewModel = hiltViewModel()) {
    val wallet by viewModel.wallet.collectAsState()
    val earnings by viewModel.earnings.collectAsState()
    val products by viewModel.products.collectAsState()
    val message by viewModel.message.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val activity = LocalContext.current.findActivity()
    var legalName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var bankCode by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Creator coins", fontWeight = FontWeight.Bold)
            Text(CreatorMoneyNotice, style = MaterialTheme.typography.bodySmall)
            Text("Coins to tip: ${wallet?.purchasedCoins ?: "…"}")
            Text("Ad points stay separate and cannot be tipped or withdrawn.")
            earnings?.let {
                Text("Held diamonds: ${it.heldDiamonds}")
                Text("Available diamonds: ${it.availableDiamonds}")
                Text("Available earnings: R${"%.2f".format(it.withdrawableCents / 100.0)} before the bank fee")
                if (it.debtCents > 0) Text("Debt from refunds: R${"%.2f".format(it.debtCents / 100.0)}")
            }
            if (products.isEmpty()) {
                Text("Coin packs are not available from Google Play on this account yet. Packs are sold in South African rand only.")
            }
            products.forEach { product ->
                OutlinedButton(
                    onClick = { activity?.let { viewModel.buy(it, product) } },
                    enabled = !busy && activity != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("${product.title}: ${product.oneTimePurchaseOfferDetails?.formattedPrice ?: "ZAR"}") }
            }
            if (earnings?.canWithdraw == true) {
                Button(onClick = viewModel::requestPayout, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Request South African payout")
                }
            }
            Text("Verify a South African bank account. The full account number is sent once to the bank and is not saved on this phone.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(legalName, { legalName = it }, label = { Text("Legal name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(accountNumber, { accountNumber = it }, label = { Text("Account number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(bankCode, { bankCode = it }, label = { Text("Bank code") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(
                onClick = {
                    viewModel.saveBank(legalName, accountNumber, bankCode)
                    accountNumber = ""
                },
                enabled = !busy && legalName.isNotBlank() && accountNumber.length >= 6 && bankCode.isNotBlank()
            ) { Text("Verify bank name") }
            if (busy) CircularProgressIndicator()
            message?.let { Text(it) }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun TipPostDialog(
    postId: String,
    onDismiss: () -> Unit,
    viewModel: CreatorWalletViewModel = hiltViewModel()
) {
    val busy by viewModel.busy.collectAsState()
    val wallet by viewModel.wallet.collectAsState()
    var coins by remember { mutableStateOf("10") }
    var note by remember { mutableStateOf<String?>(null) }
    val key = remember { UUID.randomUUID().toString() }
    LaunchedEffect(Unit) { viewModel.refresh() }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tip this post") },
        text = {
            Column {
                Text("Minimum tip is 10 purchased coins. You cannot tip your own post. Coins are not cash.")
                Text("Available coins: ${wallet?.purchasedCoins ?: "…"}")
                OutlinedTextField(coins, { coins = it.filter { ch -> ch.isDigit() }.take(6) }, label = { Text("Coins") })
                note?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && (coins.toIntOrNull() ?: 0) >= 10,
                onClick = { viewModel.tip(postId, coins.toInt(), key) { note = it } }
            ) { Text("Send tip") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
