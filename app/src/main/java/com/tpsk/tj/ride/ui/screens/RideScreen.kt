package com.tpsk.tj.ride.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.tpsk.tj.chat.ui.viewmodel.ChatViewModel
import com.tpsk.tj.common.ui.theme.AppAccent
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.drive.ui.screens.DriveDrawerContent
import com.tpsk.tj.drive.ui.screens.DriveHistoryScreen
import com.tpsk.tj.drive.ui.screens.PaymentScreen
import com.tpsk.tj.drive.ui.screens.PromotionsScreen
import com.tpsk.tj.drive.ui.screens.SafetyScreen
import com.tpsk.tj.drive.ui.screens.SavedPlacesScreen
import com.tpsk.tj.drive.ui.screens.SupportScreen
import com.tpsk.tj.drive.ui.screens.TripSummaryDialog
import com.tpsk.tj.drive.ui.viewmodel.DriveViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideScreen(
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    viewModel: DriveViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    onShowSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentSubScreen by remember { mutableStateOf<String?>(null) }
    val user = com.tpsk.tj.LocalCurrentUser.current
    val tripsState by viewModel.trips.collectAsState()

    when (currentSubScreen) {
        "history" -> {
            androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.fetchTrips() }
            DriveHistoryScreen(
                onBack = { currentSubScreen = null },
                trips = tripsState.data ?: emptyList()
            )
            return
        }
        "payment" -> {
            PaymentScreen(
                onBack = { currentSubScreen = null },
                viewModel = viewModel,
                forDriver = false,
                section = "wallet"
            )
            return
        }
        "promotions" -> {
            PromotionsScreen(onBack = { currentSubScreen = null })
            return
        }
        "safety" -> {
            SafetyScreen(onBack = { currentSubScreen = null }, viewModel = viewModel)
            return
        }
        "support" -> {
            SupportScreen(onBack = { currentSubScreen = null })
            return
        }
        "saved_places" -> {
            SavedPlacesScreen(
                onBack = { currentSubScreen = null },
                viewModel = viewModel
            )
            return
        }
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) viewModel.fetchCurrentLocation(context)
    }

    LaunchedEffect(Unit) {
        viewModel.lockRole(false)
        viewModel.setDriveActive(true)
        viewModel.watchNearbyPeople(context)
        if (!hasLocationPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            viewModel.fetchCurrentLocation(context)
        }
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.setDriveActive(false) }
    }

    val actionError = viewModel.actionError.value
    LaunchedEffect(actionError) {
        if (!actionError.isNullOrBlank()) {
            Toast.makeText(context, actionError, Toast.LENGTH_LONG).show()
            viewModel.consumeActionError()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDriverApproved by viewModel.isDriverApproved.collectAsState()
    val completedTrip by viewModel.completedTripSummary.collectAsState()
    val isSwitchingMode by viewModel.isSwitchingMode.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DriveDrawerContent(
                user = user,
                isDriverMode = false,
                isDriverApproved = isDriverApproved,
                drawerState = drawerState,
                scope = scope,
                onDriverModeToggle = { viewModel.toggleDriverMode(it) },
                roleLocked = true,
                onShowHistory = { currentSubScreen = "history"; scope.launch { drawerState.close() } },
                onShowPayment = { currentSubScreen = "payment"; scope.launch { drawerState.close() } },
                onShowPromotions = { currentSubScreen = "promotions"; scope.launch { drawerState.close() } },
                onShowSafety = { currentSubScreen = "safety"; scope.launch { drawerState.close() } },
                onShowSavedPlaces = { currentSubScreen = "saved_places"; scope.launch { drawerState.close() } },
                onShowSupport = { currentSubScreen = "support"; scope.launch { drawerState.close() } },
                onShowSettings = { onShowSettings(); scope.launch { drawerState.close() } },
                bottomPadding = bottomPadding
            )
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        RidePassengerScreen(
            viewModel = viewModel,
            userName = user?.displayName?.substringBefore(" ")?.ifBlank { null } ?: "there",
            onMenuClick = { scope.launch { drawerState.open() } },
            chatViewModel = chatViewModel,
            bottomPadding = bottomPadding
        )
    }

    if (completedTrip != null) {
        TripSummaryDialog(
            summary = completedTrip!!,
            onDismiss = {
                viewModel.clearTripSummary()
                viewModel.updateSelectedDestination(null)
            }
        )
    }

    if (isSwitchingMode) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = AppAccent)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Switching roles...", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
