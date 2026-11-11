package com.tpsk.tj.stokvel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.stokvel.data.model.StokvelType
import com.tpsk.tj.stokvel.data.remote.CreateGroupRequestDto
import com.tpsk.tj.stokvel.ui.viewmodel.StokvelViewModel

private val DarkBackground = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val PrimaryBlue = Color(0xFF2563EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStokvelScreen(
    onBack: () -> Unit,
    viewModel: StokvelViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(StokvelType.UMGALELO_ROTATION) }
    var targetAmount by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accountHint by remember { mutableStateOf("") }
    var payshapId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Stokvel", fontWeight = FontWeight.Bold, color = Color.White) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Stokvel Group Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    focusedLabelColor = PrimaryBlue
                )
            )

            Text("Stokvel Model / Type", fontWeight = FontWeight.SemiBold, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StokvelType.entries.forEach { type ->
                    val selected = type == selectedType
                    Button(
                        onClick = { selectedType = type },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) PrimaryBlue else CardBackground,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (type) {
                                StokvelType.UMGALELO_ROTATION -> "Rotation"
                                StokvelType.UMTSHELO_PARTY -> "Party"
                                StokvelType.BULK_GROCERIES -> "Bulk Groceries"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            OutlinedTextField(
                value = targetAmount,
                onValueChange = { targetAmount = it },
                label = { Text("Monthly Target Amount (ZAR R)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    focusedLabelColor = PrimaryBlue
                )
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Optional Bank & PayShap Ledger Details", fontWeight = FontWeight.SemiBold, color = PrimaryBlue)

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name (e.g. Capitec, Standard Bank)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountHint,
                        onValueChange = { accountHint = it },
                        label = { Text("Masked Account Hint (e.g. ••••4567)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = payshapId,
                        onValueChange = { payshapId = it },
                        label = { Text("PayShap ID / Cell Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val amount = targetAmount.toDoubleOrNull()
                    if (name.isBlank() || amount == null || amount <= 0) {
                        errorMessage = "Please enter a valid group name and target amount."
                        return@Button
                    }
                    viewModel.createStokvel(
                        CreateGroupRequestDto(
                            name = name,
                            type = selectedType.name.lowercase(),
                            monthlyTargetAmount = amount,
                            creatorUserId = "usr-1",
                            bankName = bankName.ifBlank { null },
                            accountNumberHint = accountHint.ifBlank { null },
                            payshapId = payshapId.ifBlank { null }
                        )
                    )
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Create Stokvel Group", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
