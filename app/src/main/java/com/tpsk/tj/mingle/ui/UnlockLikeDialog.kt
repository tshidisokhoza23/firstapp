package com.tpsk.tj.mingle.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val PrimaryBlue = Color(0xFF2563EB)
private val AccentGold = Color(0xFFF59E0B)

@Composable
fun UnlockLikeDialog(
    targetName: String,
    coinBalance: Int,
    onDismissRequest: () -> Unit,
    onConfirmUnlock: () -> Unit,
    onBuyCoins: () -> Unit
) {
    val canAfford = coinBalance >= 5

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Unblur Who Liked You?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Spend 5 Coins to permanently unlock and view $targetName's clear profile & photos."
                )
                Text(
                    text = "Current Balance: $coinBalance Coins",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentGold,
                    fontWeight = FontWeight.SemiBold
                )
                if (!canAfford) {
                    Text(
                        text = "You need 5 coins to unblur this profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canAfford) {
                        onConfirmUnlock()
                    } else {
                        onBuyCoins()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = if (canAfford) "Unlock for 5 Coins" else "Get Coins",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
