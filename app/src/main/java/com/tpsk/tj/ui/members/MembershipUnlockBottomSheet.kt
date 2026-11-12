package com.tpsk.tj.ui.members

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembershipUnlockBottomSheet(
    membership: MembershipItem,
    onDismiss: () -> Unit,
    onLaunchGooglePlayBilling: (sku: String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(membership.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text("by @${membership.creatorHandle} • ${membership.memberCount} members", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            
            Spacer(Modifier.height(20.dp))
            Text("Unlock VIP Access", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("💬 Subscriber-only group chat", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text("✓ Exclusive daily voice notes & spaces", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text("✓ Custom VIP subscriber badge", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Monthly Subscription", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("R${membership.priceCents / 100}.00 / month", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("Auto-renews monthly. Cancel anytime.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { membership.sku?.let { onLaunchGooglePlayBilling(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Subscribe with Google Play",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Secured by Google Play. 15% Google, 10% TJ, 75% Creator split.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
