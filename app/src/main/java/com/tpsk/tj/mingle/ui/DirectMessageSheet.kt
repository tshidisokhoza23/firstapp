package com.tpsk.tj.mingle.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val DarkCharcoal = Color(0xFF0F172A)
private val PrimaryBlue = Color(0xFF2563EB)
private val AccentGold = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectMessageSheet(
    targetName: String,
    coinBalance: Int,
    onDismissRequest: () -> Unit,
    onSendDirectMessage: (String) -> Unit,
    onBuyCoins: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val canAfford = coinBalance >= 5

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = DarkCharcoal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Send Direct Message",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "Skip the match queue and message $targetName directly for 5 Coins. Limited to 1 pending message (Harassment Shield).",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray
            )

            OutlinedTextField(
                value = messageText,
                onValueChange = { if (it.length <= 300) messageText = it },
                label = { Text("Your Message") },
                placeholder = { Text("Say something genuine...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    focusedLabelColor = PrimaryBlue
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Costs 5 Coins • Balance: $coinBalance",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${messageText.length}/300",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Button(
                onClick = {
                    if (canAfford) {
                        if (messageText.isNotBlank()) {
                            onSendDirectMessage(messageText)
                        }
                    } else {
                        onBuyCoins()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = canAfford && messageText.isNotBlank()
            ) {
                Text(
                    text = if (canAfford) "Send Intro (5 Coins)" else "Get Coins",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
