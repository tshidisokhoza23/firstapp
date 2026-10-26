package com.tpsk.tj.drive.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tpsk.tj.common.ui.theme.AppAccent
import com.tpsk.tj.common.ui.theme.AppBackground
import com.tpsk.tj.common.ui.theme.AppSecondary
import com.tpsk.tj.common.ui.theme.AppTextSecondary
import com.tpsk.tj.drive.ui.viewmodel.DriveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promotions", fontWeight = FontWeight.Bold) },
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
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("No promotions right now", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            item {
                Text(
                    "There is no promo code to enter. The price on the request screen is the live fare: R20 plus the vehicle rate per kilometre plus R1.50 per traffic minute. Waiting is free for 3 minutes, then R2 per minute.",
                    color = AppTextSecondary,
                    lineHeight = 20.sp
                )
            }
            item {
                Text("Card and 1Voucher only. Cash is not accepted.", color = AppAccent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(onBack: () -> Unit, viewModel: DriveViewModel) {
    BackHandler { onBack() }
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety", fontWeight = FontWeight.Bold) },
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
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Use these during a trip. They stay on this screen.", color = AppTextSecondary)
            Spacer(Modifier.height(8.dp))
            SafetyActionItem(Icons.Default.ShareLocation, "Share trip", "Send your live status to someone you trust") {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "I am on a TJ trip. Arrival is ${viewModel.remainingTime.value ?: "being updated"}.")
                }
                runCatching { context.startActivity(Intent.createChooser(share, "Share trip")) }
                    .onFailure { Toast.makeText(context, "Could not open share", Toast.LENGTH_SHORT).show() }
            }
            SafetyActionItem(Icons.Default.LocalPolice, "Emergency", "Opens the phone dialer for 112", Color(0xFFEA4335)) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }.onFailure { Toast.makeText(context, "Could not open the dialer", Toast.LENGTH_SHORT).show() }
            }
            SafetyActionItem(Icons.Default.Lock, "Passenger PIN", "The driver cannot start the trip until you give them the PIN") {
                val pin = viewModel.safetyPin.value
                Toast.makeText(
                    context,
                    if (pin.isNullOrBlank()) "Your PIN appears here after a driver accepts." else "Give the driver PIN $pin",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
