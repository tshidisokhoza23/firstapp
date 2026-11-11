package com.tpsk.tj.mingle.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkCharcoal = Color(0xFF0F172A)
private val CardDark = Color(0xFF1E293B)
private val PrimaryBlue = Color(0xFF2563EB)
private val AccentGold = Color(0xFFF59E0B)

data class CoinPackageUi(
    val productId: String,
    val title: String,
    val coins: Int,
    val priceZar: String,
    val isBestValue: Boolean = false
)

val CoinPackagesList = listOf(
    CoinPackageUi("tj_coins_10", "Starter Pack", 10, "ZAR 29.99"),
    CoinPackageUi("tj_coins_18", "Value Pack", 18, "ZAR 49.99"),
    CoinPackageUi("tj_coins_27", "Popular Pack", 27, "ZAR 69.99", isBestValue = true),
    CoinPackageUi("tj_coins_42", "Pro Pack", 42, "ZAR 99.99"),
    CoinPackageUi("tj_coins_70", "VIP Mega Pack", 70, "ZAR 149.99")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinPurchaseSheet(
    currentBalance: Int,
    onDismissRequest: () -> Unit,
    onSelectPackage: (CoinPackageUi) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = DarkCharcoal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "TJ Coin Store",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "Current Balance: $currentBalance Coins",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AccentGold
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(CoinPackagesList) { pkg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = if (pkg.isBestValue) BorderStroke(1.5.dp, AccentGold) else null,
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = pkg.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    if (pkg.isBestValue) {
                                        Surface(
                                            color = AccentGold.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "BEST VALUE",
                                                color = AccentGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${pkg.coins} Coins",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.LightGray
                                )
                            }

                            Button(
                                onClick = { onSelectPackage(pkg) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = pkg.priceZar,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
