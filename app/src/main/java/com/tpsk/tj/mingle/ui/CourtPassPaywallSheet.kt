package com.tpsk.tj.mingle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CourtPassTier(
    val title: String,
    val priceLabel: String,
    val durationText: String,
    val dmTokens: Int,
    val badgeText: String?
) {
    DAILY("Daily Pass", "R25.00 / 24h", "24 Hours Access", 2, null),
    WEEKLY("Weekly Pass", "R49.00 / 7 days", "7 Days Access", 7, "POPULAR"),
    MONTHLY("Monthly Pass", "R100.00 / 30 days", "30 Days Access", 25, "BEST VALUE")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourtPassPaywallSheet(
    onDismissRequest: () -> Unit,
    onPurchaseSuccess: (CourtPassTier) -> Unit
) {
    var selectedTier by remember { mutableStateOf(CourtPassTier.WEEKLY) }
    var isProcessing by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Crown Header Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFFEF3C7), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Court Pass",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Unlock Who Liked You & Direct Note Opener Tokens",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Perks List
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CourtPerkRow(
                        icon = Icons.Default.Visibility,
                        color = Color(0xFFA855F7),
                        title = "Unlimited 'Who Liked Me' Unblur",
                        desc = "See everyone who liked your profile instantly."
                    )
                    CourtPerkRow(
                        icon = Icons.AutoMirrored.Filled.Send,
                        color = Color(0xFF3B82F6),
                        title = "Direct Notes (Unmatched DMs)",
                        desc = "Send 140-char direct openers to anyone."
                    )
                    CourtPerkRow(
                        icon = Icons.Default.Bolt,
                        color = Color(0xFFEAB308),
                        title = "Priority Visibility",
                        desc = "Get featured at the top of local feeds."
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tiers Selector
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CourtPassTier.entries.forEach { tier ->
                    val isSelected = selectedTier == tier
                    Surface(
                        onClick = { selectedTier = tier },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = tier.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    tier.badgeText?.let { badge ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (tier == CourtPassTier.MONTHLY) Color(0xFF16A34A) else Color(0xFF2563EB)
                                        ) {
                                            Text(
                                                text = badge,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "${tier.durationText} • ${tier.dmTokens} Direct Note Tokens",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Text(
                                text = tier.priceLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Unlock Button
            Button(
                onClick = {
                    if (isProcessing) return@Button
                    isProcessing = true
                    onPurchaseSuccess(selectedTier)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Unlock ${selectedTier.title} • ${selectedTier.priceLabel}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CourtPerkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
