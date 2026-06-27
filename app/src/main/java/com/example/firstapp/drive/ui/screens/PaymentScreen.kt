package com.example.firstapp.drive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import java.util.Locale
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.drive.ui.viewmodel.DriveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(onBack: () -> Unit, viewModel: DriveViewModel) {
    BackHandler { onBack() }
    var showAddCard by remember { mutableStateOf(false) }
    var showAddPromo by remember { mutableStateOf(false) }
    val balance by viewModel.walletBalance
    val transactions = viewModel.transactions

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment", fontWeight = FontWeight.Bold) },
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
                        Text("Wallet Balance", color = AppTextSecondary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("R ${String.format(Locale.getDefault(), "%.2f", balance)}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* Top up */ },
                            colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Top Up", color = AppBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(
                    "Payment Methods",
                    modifier = Modifier.padding(16.dp),
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                PaymentMethodItem(
                    icon = Icons.Default.Payments,
                    title = "Cash",
                    subtitle = "Default",
                    isSelected = true
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                PaymentMethodItem(
                    icon = Icons.Default.CreditCard,
                    title = "•••• 4242",
                    subtitle = "Expires 12/26",
                    isSelected = false
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddCard = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AppAccent)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Add Payment Method", color = AppAccent, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Promotions",
                    modifier = Modifier.padding(16.dp),
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    onClick = { showAddPromo = true },
                    color = AppSecondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = AppAccent)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Add Promo Code", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Recent Transactions",
                    modifier = Modifier.padding(16.dp),
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(transactions) { tx ->
                TransactionItem(tx)
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAddCard) {
        AddCardDialog(onDismiss = { showAddCard = false })
    }
    if (showAddPromo) {
        AddPromoDialog(onDismiss = { showAddPromo = false })
    }
}

@Composable
fun AddPromoDialog(onDismiss: () -> Unit) {
    var promoCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = { Text("Add Promo Code", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = promoCode,
                onValueChange = { promoCode = it.uppercase() },
                placeholder = { Text("Enter code (e.g. FIRSTAPP50)", color = AppTextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppAccent,
                    unfocusedBorderColor = Color.White.copy(0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
            ) {
                Text("Apply", color = AppBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTextSecondary)
            }
        }
    )
}

@Composable
fun AddCardDialog(onDismiss: () -> Unit) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = { Text("Add New Card", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 16) cardNumber = it },
                    label = { Text("Card Number", color = AppTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = Color.White.copy(0.1f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { if (it.length <= 5) expiry = it },
                        label = { Text("MM/YY", color = AppTextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppAccent,
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { if (it.length <= 3) cvv = it },
                        label = { Text("CVV", color = AppTextSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppAccent,
                            unfocusedBorderColor = Color.White.copy(0.1f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
            ) {
                Text("Add Card", color = AppBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTextSecondary)
            }
        }
    )
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
fun TransactionItem(tx: com.example.firstapp.drive.model.Transaction) {
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
