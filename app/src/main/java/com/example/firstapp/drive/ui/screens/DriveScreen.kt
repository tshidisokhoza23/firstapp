package com.example.firstapp.drive.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.drive.model.Destination
import com.example.firstapp.drive.model.RideOption
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.drive.ui.viewmodel.DriveViewModel
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    viewModel: DriveViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    chatViewModel: com.example.firstapp.chat.ui.viewmodel.ChatViewModel = hiltViewModel(),
    onShowSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentSubScreen by remember { mutableStateOf<String?>(null) }
    val user by userViewModel.currentUser.collectAsState()
    val tripsState by viewModel.trips.collectAsState()

    when (currentSubScreen) {
        "history" -> {
            DriveHistoryScreen(
                onBack = { currentSubScreen = null },
                trips = tripsState.data ?: emptyList()
            )
            return
        }
        "payment" -> {
            PaymentScreen(
                onBack = { currentSubScreen = null },
                viewModel = viewModel
            )
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
        if (isGranted) {
            viewModel.fetchCurrentLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            viewModel.fetchCurrentLocation(context)
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var step by remember { mutableIntStateOf(0) }
    var isSearching by remember { mutableStateOf(false) }
    var showSafetyCenter by remember { mutableStateOf(false) }
    var showDriverProfile by remember { mutableStateOf(false) }
    var showSchedulePicker by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<Destination?>(null) }
    val additionalStops = remember { mutableStateListOf<Destination>() }
    val selectedRide by viewModel.selectedRide
    var fromLocation by remember { mutableStateOf("Current Location") }
    var toLocation by remember { mutableStateOf("") }
    
    val pickupAddress by viewModel.pickupAddress
    LaunchedEffect(pickupAddress) {
        fromLocation = pickupAddress
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { 
            DriveDrawerContent(
                user = user,
                onShowHistory = { currentSubScreen = "history"; scope.launch { drawerState.close() } },
                onShowPayment = { currentSubScreen = "payment"; scope.launch { drawerState.close() } },
                onShowSavedPlaces = { currentSubScreen = "saved_places"; scope.launch { drawerState.close() } },
                onShowSupport = { currentSubScreen = "support"; scope.launch { drawerState.close() } },
                onShowSettings = { onShowSettings(); scope.launch { drawerState.close() } }
            )
        },
        gesturesEnabled = drawerState.isOpen
    ) {
        DriveMainContent(
            scaffoldState = scaffoldState,
            hasLocationPermission = hasLocationPermission,
            step = step,
            onStepChange = { step = it },
            isSearchingView = isSearching,
            onSearchToggle = { isSearching = it },
            selectedDestination = selectedDestination,
            onDestinationSelected = { destination ->
                if (destination != null) {
                    viewModel.getPlaceDetails(context, destination) { detailedDest ->
                        selectedDestination = detailedDest
                        step = 1
                        isSearching = false
                        scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                        
                        val pickup = viewModel.pickupLocation.value ?: LatLng(-26.1076, 28.0567)
                        val waypoints = additionalStops.map { LatLng(it.latitude, it.longitude) }
                        viewModel.calculateRoute(pickup, LatLng(detailedDest.latitude, detailedDest.longitude), waypoints)
                        
                        viewModel.saveDestination(
                            name = detailedDest.name,
                            address = detailedDest.address,
                            latitude = detailedDest.latitude,
                            longitude = detailedDest.longitude,
                            type = "recent"
                        )
                    }
                } else {
                    selectedDestination = null
                }
            },
            additionalStops = additionalStops,
            onAddStop = { additionalStops.add(it) },
            onRemoveStop = { additionalStops.removeAt(it) },
            selectedRide = selectedRide,
            onRideSelect = { viewModel.selectRide(it) },
            fromLocation = fromLocation,
            onFromLocationChange = {
                fromLocation = it
                viewModel.searchPlaces(context, it)
            },
            toLocation = toLocation,
            onToLocationChange = { 
                toLocation = it
                viewModel.searchPlaces(context, it)
            },
            onMenuClick = { scope.launch { drawerState.open() } },
            showSafetyCenter = showSafetyCenter,
            onSafetyCenterToggle = { showSafetyCenter = it },
            showDriverProfile = showDriverProfile,
            onDriverProfileToggle = { showDriverProfile = it },
            showSchedulePicker = showSchedulePicker,
            onSchedulePickerToggle = { showSchedulePicker = it },
            bottomPadding = bottomPadding,
            viewModel = viewModel,
            user = user,
            chatViewModel = chatViewModel
        )
    }

    if (showSafetyCenter) {
        SafetyCenterDialog(
            onDismiss = { showSafetyCenter = false },
            context = context,
            viewModel = viewModel
        )
    }

    if (showDriverProfile) {
        DriverProfileDialog(
            onDismiss = { showDriverProfile = false },
            driver = viewModel.assignedDriver.value ?: viewModel.nearbyDrivers.value.firstOrNull()
        )
    }

    if (showSchedulePicker) {
        ScheduleRideDialog(
            onDismiss = { showSchedulePicker = false },
            onSchedule = { date, time ->
                Toast.makeText(context, "Ride scheduled for $date at $time", Toast.LENGTH_LONG).show()
                showSchedulePicker = false
            }
        )
    }
}

@Composable
fun DriveDrawerContent(
    user: com.example.firstapp.common.model.User?,
    onShowHistory: () -> Unit, 
    onShowPayment: () -> Unit, 
    onShowSavedPlaces: () -> Unit,
    onShowSupport: () -> Unit, 
    onShowSettings: () -> Unit
) {
    val context = LocalContext.current
    ModalDrawerSheet(
        drawerContainerColor = AppSecondary,
        modifier = Modifier.width(320.dp),
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AppAccent.copy(0.15f), Color.Transparent)))
                .padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AppPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (user?.avatar_url != null) {
                        coil.compose.AsyncImage(
                            model = user.avatar_url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            user?.display_name?.take(1)?.uppercase() ?: "U", 
                            color = Color.White, 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 28.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(user?.display_name ?: "Guest", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = NiceOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${user?.streak ?: 0} Streak • Gold Member", color = AppTextSecondary, fontSize = 14.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        DrawerItem(Icons.AutoMirrored.Filled.List, "My Trips") { onShowHistory() }
        DrawerItem(Icons.Default.Place, "Saved Places") { onShowSavedPlaces() }
        DrawerItem(Icons.Default.Payment, "Wallet") { onShowPayment() }
        DrawerItem(Icons.Default.CardGiftcard, "Promotions") { onShowPayment() }
        DrawerItem(Icons.Default.SupportAgent, "Help Center") { onShowSupport() }
        DrawerItem(Icons.Default.Info, "Legal") { Toast.makeText(context, "FirstApp Drive v1.2", Toast.LENGTH_SHORT).show() }
        
        Spacer(modifier = Modifier.weight(1f))
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 24.dp))
        DrawerItem(Icons.Default.Settings, "Settings") { onShowSettings() }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun DrawerItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    NavigationDrawerItem(
        label = { Text(label, color = Color.White, fontWeight = FontWeight.Medium) },
        selected = false,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null, tint = Color.White.copy(0.7f)) },
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveMainContent(
    scaffoldState: BottomSheetScaffoldState,
    hasLocationPermission: Boolean,
    step: Int,
    onStepChange: (Int) -> Unit,
    isSearchingView: Boolean,
    onSearchToggle: (Boolean) -> Unit,
    selectedDestination: Destination?,
    onDestinationSelected: (Destination?) -> Unit,
    additionalStops: List<Destination>,
    onAddStop: (Destination) -> Unit,
    onRemoveStop: (Int) -> Unit,
    selectedRide: RideOption?,
    onRideSelect: (RideOption?) -> Unit,
    fromLocation: String,
    onFromLocationChange: (String) -> Unit,
    toLocation: String,
    onToLocationChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    showSafetyCenter: Boolean,
    onSafetyCenterToggle: (Boolean) -> Unit,
    showDriverProfile: Boolean,
    onDriverProfileToggle: (Boolean) -> Unit,
    showSchedulePicker: Boolean,
    onSchedulePickerToggle: (Boolean) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    viewModel: DriveViewModel,
    user: com.example.firstapp.common.model.User?,
    chatViewModel: com.example.firstapp.chat.ui.viewmodel.ChatViewModel
) {
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    
    val pickupLocation = viewModel.pickupLocation.value ?: LatLng(-26.1076, 28.0567)

    BackHandler(enabled = isSearchingView || step > 0) {
        if (isSearchingView) {
            onSearchToggle(false)
            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
        } else if (step > 0) {
            onStepChange(step - 1)
        }
    }

    val destCoords = remember(selectedDestination) {
        if (selectedDestination != null) {
            LatLng(selectedDestination.latitude, selectedDestination.longitude)
        } else pickupLocation
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(pickupLocation, 14f)
    }

    LaunchedEffect(cameraPositionState.isMoving, step) {
        if (!cameraPositionState.isMoving && step == 0 && !isSearchingView) {
            viewModel.reverseGeocode(context, cameraPositionState.position.target)
        }
    }

    val routePoints = viewModel.routePoints.value

    val currentPeekHeight = remember(step, isSearchingView, bottomPadding) {
        when {
            step == 2 -> 360.dp + bottomPadding
            else -> 140.dp + bottomPadding
        }
    }

    val sheetHeight = remember(scaffoldState.bottomSheetState.currentValue, scaffoldState.bottomSheetState.targetValue) {
        if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) 600.dp else currentPeekHeight
    }

    val mapPadding = with(LocalDensity.current) { sheetHeight.toPx().toInt() }

    LaunchedEffect(pickupLocation) {
        if (selectedDestination == null) {
            cameraPositionState.animate(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(pickupLocation, 14f))
        }
    }

    LaunchedEffect(step) {
        when(step) {
            0 -> scaffoldState.bottomSheetState.partialExpand()
            1 -> scaffoldState.bottomSheetState.expand()
            2 -> scaffoldState.bottomSheetState.partialExpand()
        }
    }

    LaunchedEffect(selectedDestination, routePoints, pickupLocation) {
        if (selectedDestination != null && routePoints.isNotEmpty()) {
            val bounds = LatLngBounds.builder()
                .apply {
                    routePoints.forEach { include(it) }
                }
                .build()
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 200),
                1000
            )
        } else if (selectedDestination != null) {
            val bounds = LatLngBounds.builder()
                .include(pickupLocation)
                .include(destCoords)
                .build()
            cameraPositionState.animate(
                com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 200),
                1000
            )
        }
    }

    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = scaffoldState,
        sheetPeekHeight = currentPeekHeight,
        sheetContainerColor = AppSecondary,
        sheetContentColor = Color.White,
        sheetShadowElevation = 24.dp,
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(0.2f), CircleShape)
            )
        },
        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isSearchingView || step == 1) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp + bottomPadding)
                    .animateContentSize()
            ) {
                when {
                    step == 0 -> SearchFlow(
                        isSearching = isSearchingView,
                        onSearchToggle = { 
                            onSearchToggle(it)
                            scope.launch {
                                if (it) {
                                    scaffoldState.bottomSheetState.expand()
                                } else {
                                    scaffoldState.bottomSheetState.partialExpand()
                                }
                            }
                        },
                        fromLocation = fromLocation,
                        onFromChange = onFromLocationChange,
                        toLocation = toLocation,
                        onToChange = onToLocationChange,
                        additionalStops = additionalStops,
                        onAddStop = onAddStop,
                        onRemoveStop = onRemoveStop,
                        focusRequester = focusRequester,
                        onDestinationSelected = { dest ->
                            onDestinationSelected(dest)
                            scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                        },
                        onPickupSelected = { dest ->
                            viewModel.getPlaceDetails(context, dest) { detailed ->
                                onFromLocationChange(detailed.name)
                                viewModel.updatePickupLocation(LatLng(detailed.latitude, detailed.longitude), detailed.name)
                                selectedDestination?.let {
                                    val waypoints = additionalStops.map { LatLng(it.latitude, it.longitude) }
                                    viewModel.calculateRoute(LatLng(detailed.latitude, detailed.longitude), LatLng(it.latitude, it.longitude), waypoints)
                                }
                            }
                        },
                        modifier = if (isSearchingView) Modifier.fillMaxSize() else Modifier.wrapContentHeight(),
                        viewModel = viewModel,
                        user = user
                    )
                    step == 1 -> RideSelectionFlow(
                        destination = selectedDestination,
                        selectedRide = selectedRide,
                        onRideSelect = { onRideSelect(it) },
                        onBook = { 
                            scope.launch {
                                scaffoldState.bottomSheetState.partialExpand()
                            }
                            onStepChange(2) 
                            viewModel.startBooking()
                        },
                        onScheduleClick = { onSchedulePickerToggle(true) },
                        modifier = Modifier.fillMaxHeight(),
                        viewModel = viewModel
                    )
                    step == 2 -> LiveTrackingFlow(
                        onCancel = { 
                            onStepChange(0)
                            onDestinationSelected(null)
                            onRideSelect(null)
                            viewModel.clearRoute()
                            viewModel.cancelBooking()
                        },
                        onShowDriverProfile = { onDriverProfileToggle(true) },
                        viewModel = viewModel,
                        chatViewModel = chatViewModel
                    )
                }
            }
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                contentPadding = PaddingValues(bottom = sheetHeight),
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    mapType = MapType.NORMAL,
                    mapStyleOptions = MapStyleOptions(mapStyleJson)
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
            ) {
                val nearbyDrivers = viewModel.nearbyDrivers.value
                val assignedDriver = viewModel.assignedDriver.value
                val carIcon = remember { createCarBitmap(context) }

                // Pickup Marker - only show when booking or tracking
                if (selectedDestination != null && step > 0) {
                    val pickupMarkerState = rememberMarkerState(position = pickupLocation)
                    LaunchedEffect(pickupLocation) {
                        pickupMarkerState.position = pickupLocation
                    }
                    
                    Marker(
                        state = pickupMarkerState,
                        title = "Pickup Location",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        zIndex = 1f
                    )
                }

                nearbyDrivers.forEach { driver ->
                    key(driver.id) {
                        val markerState = rememberMarkerState(position = driver.position)
                        LaunchedEffect(driver.position) {
                            markerState.position = driver.position
                        }
                        Marker(
                            state = markerState,
                            rotation = driver.rotation,
                            flat = true,
                            icon = carIcon,
                            anchor = Offset(0.5f, 0.5f),
                            onClick = { onDriverProfileToggle(true); true }
                        )
                    }
                }

                if (nearbyDrivers.isEmpty()) {
                    // Force a dummy driver if simulation hasn't started or no drivers nearby
                    Marker(
                        state = rememberMarkerState(position = LatLng(pickupLocation.latitude + 0.002, pickupLocation.longitude + 0.002)),
                        rotation = 45f,
                        flat = true,
                        icon = carIcon,
                        anchor = Offset(0.5f, 0.5f)
                    )
                }

                assignedDriver?.let { driver ->
                    key("assigned_driver") {
                        val markerState = rememberMarkerState(position = driver.position)
                        LaunchedEffect(driver.position) {
                            markerState.position = driver.position
                        }
                        Marker(
                            state = markerState,
                            rotation = driver.rotation,
                            flat = true,
                            icon = carIcon,
                            anchor = Offset(0.5f, 0.5f),
                            zIndex = 10f,
                            onClick = { onDriverProfileToggle(true); true }
                        )
                    }
                }

                if (selectedDestination != null && step > 0) {
                    val destMarkerState = rememberMarkerState(position = destCoords)
                    LaunchedEffect(destCoords) {
                        destMarkerState.position = destCoords
                    }
                    Marker(
                        state = destMarkerState,
                        title = "Destination",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    additionalStops.forEachIndexed { index, stop ->
                        Marker(
                            state = rememberMarkerState(position = LatLng(stop.latitude, stop.longitude)),
                            title = "Stop ${index + 1}",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                        )
                    }

                    if (routePoints.isNotEmpty()) {
                        val trafficSegments = viewModel.trafficSegments.value
                        if (trafficSegments.isNotEmpty()) {
                            trafficSegments.forEach { segment ->
                                Polyline(
                                    points = segment.points,
                                    color = segment.color,
                                    width = 12f,
                                    geodesic = true,
                                    jointType = JointType.ROUND,
                                    startCap = RoundCap(),
                                    endCap = RoundCap()
                                )
                            }
                        } else {
                            Polyline(
                                points = routePoints,
                                color = AppAccent,
                                width = 12f,
                                geodesic = true,
                                jointType = JointType.ROUND,
                                startCap = RoundCap(),
                                endCap = RoundCap()
                            )
                        }
                    }
                }
            }
            
            // Map overlay buttons
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                if (!isSearchingView && step == 0) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.background(AppSecondary, CircleShape).size(48.dp)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                } else {
                    IconButton(
                        onClick = { 
                            if (isSearchingView) {
                                onSearchToggle(false)
                                scope.launch { scaffoldState.bottomSheetState.partialExpand() }
                            } else {
                                val nextStep = step - 1
                                onStepChange(nextStep)
                                if (nextStep == 0) {
                                    onDestinationSelected(null)
                                    viewModel.clearRoute()
                                }
                            }
                        },
                        modifier = Modifier.background(AppSecondary, CircleShape).size(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (step == 0 && !isSearchingView) {
                    IconButton(
                        onClick = { onSafetyCenterToggle(true) },
                        modifier = Modifier.background(AppSecondary, CircleShape).size(48.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = "Safety", tint = AppAccent)
                    }
                }
            }
            
            if (step == 0 && !isSearchingView) {
                FloatingActionButton(
                    onClick = { 
                        scope.launch { 
                            cameraPositionState.animate(com.google.android.gms.maps.CameraUpdateFactory.newLatLng(pickupLocation)) 
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp),
                    containerColor = AppSecondary,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }
            }

            if (step == 0 && !isSearchingView) {
                // Center Pin for Pickup
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = sheetHeight), // Match map padding
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.offset(y = (-20).dp)
                    ) {
                        Surface(
                            color = AppAccent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                "PICKUP HERE",
                                color = AppBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AppAccent,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SafetyCenterDialog(onDismiss: () -> Unit, context: Context, viewModel: DriveViewModel) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = AppAccent)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Safety Center", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SafetyActionItem(
                    icon = Icons.Default.ShareLocation,
                    title = "Share Trip Status",
                    description = "Let friends and family track your journey",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "I am currently on a FirstApp Drive trip. My ETA is ${viewModel.remainingTime.value ?: "soon"}.")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share trip"))
                    }
                )
                SafetyActionItem(
                    icon = Icons.Default.LocalPolice,
                    title = "Emergency Assistance",
                    description = "Connect with local emergency services",
                    color = Color(0xFFEA4335),
                    onClick = {
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                SafetyActionItem(
                    icon = Icons.Default.SupportAgent,
                    title = "Safety Support",
                    description = "Report a safety concern to our team",
                    onClick = {
                        Toast.makeText(context, "Safety support requested", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AppTextSecondary)
            }
        }
    )
}

@Composable
fun SafetyActionItem(
 icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    color: Color = Color.White,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(description, color = AppTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun DriverProfileDialog(onDismiss: () -> Unit, driver: com.example.firstapp.drive.ui.viewmodel.NearbyDriver?) {
    if (driver == null) return
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(AppAccent.copy(0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = AppAccent)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(driver.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = NiceOrange, modifier = Modifier.size(14.dp))
                        Text(" ${driver.rating}", color = AppTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailRow("Vehicle", driver.vehicleModel)
                DetailRow("Color", driver.vehicleColor)
                DetailRow("Plate", driver.licensePlate)
                
                HorizontalDivider(color = Color.White.copy(0.05f))
                
                Text(
                    "Top Rated Driver • 500+ Trips",
                    color = AppAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AppTextSecondary)
            }
        }
    )
}

private fun createCarBitmap(context: Context): BitmapDescriptor {
 val size = (32 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // Shadow
    paint.color = android.graphics.Color.BLACK
    paint.alpha = 40
    canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint)
    
    // Car Body
    paint.alpha = 255
    paint.color = AppAccent.toArgb()
    canvas.drawRoundRect(
        size * 0.25f, size * 0.15f, size * 0.75f, size * 0.85f, 
        size * 0.15f, size * 0.15f, paint
    )
    
    // Windshield
    paint.color = android.graphics.Color.WHITE
    paint.alpha = 180
    canvas.drawRect(size * 0.35f, size * 0.25f, size * 0.65f, size * 0.35f, paint)
    
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private val mapStyleJson = """
[
  {
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#212121"
      }
    ]
  },
  {
    "featureType": "poi",
    "elementType": "labels.icon",
    "stylers": [
      {
        "visibility": "off"
      }
    ]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#757575"
      }
    ]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [
      {
        "color": "#212121"
      }
    ]
  },
  {
    "featureType": "administrative",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#757575"
      }
    ]
  },
  {
    "featureType": "administrative.country",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#9e9e9e"
      }
    ]
  },
  {
    "featureType": "poi",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#757575"
      }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry.fill",
    "stylers": [
      {
        "color": "#2c2c2c"
      }
    ]
  },
  {
    "featureType": "road",
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#8a8a8a"
      }
    ]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#000000"
      }
    ]
  }
]
""".trimIndent()

@Composable
fun SearchFlow(
    isSearching: Boolean,
    onSearchToggle: (Boolean) -> Unit,
    fromLocation: String,
    onFromChange: (String) -> Unit,
    toLocation: String,
    onToChange: (String) -> Unit,
    additionalStops: List<Destination>,
    onAddStop: (Destination) -> Unit,
    onRemoveStop: (Int) -> Unit,
    focusRequester: FocusRequester,
    onDestinationSelected: (Destination) -> Unit,
    onPickupSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DriveViewModel,
    user: com.example.firstapp.common.model.User?
) {
    val context = LocalContext.current
    val destinationsState by viewModel.destinations.collectAsState()
    val recentItems = remember(destinationsState) {
        destinationsState.data?.filter { it.type == com.example.firstapp.drive.model.DestinationType.RECENT } ?: emptyList()
    }
    val favorites = remember(destinationsState) {
        destinationsState.data?.filter { it.isFavorite } ?: emptyList()
    }

    val isSearchingAddress = viewModel.isSearching.value
    val suggestions = viewModel.suggestions.value
    var focusedField by remember { mutableIntStateOf(1) } // 0 for from, 1 for to

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    Column(modifier = modifier) {
        if (!isSearching) {
            Surface(
                onClick = { onSearchToggle(true) },
                color = Color(0xFF233138),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AppAccent, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Where to?", color = AppTextSecondary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Good morning, ${user?.display_name?.substringBefore(" ") ?: "Guest"}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val home = favorites.find { it.type == com.example.firstapp.drive.model.DestinationType.HOME }
                val work = favorites.find { it.type == com.example.firstapp.drive.model.DestinationType.WORK }

                QuickActionChip(Icons.Default.Home, home?.name ?: "Home") { 
                    home?.let { onDestinationSelected(it) } ?: onSearchToggle(true)
                }
                QuickActionChip(Icons.Default.Work, work?.name ?: "Work") { 
                    work?.let { onDestinationSelected(it) } ?: onSearchToggle(true)
                }
                
                IconButton(
                    onClick = { onSearchToggle(true) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF233138), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add favorite", tint = AppAccent)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Plan your trip", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    IconButton(onClick = { onSearchToggle(false) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(0.7f))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF233138), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(start = 16.dp).width(24.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF4A89F3), CircleShape))
                            Box(modifier = Modifier.width(1.dp).height(if (additionalStops.isEmpty()) 50.dp else (50 + additionalStops.size * 60).dp).background(Color.White.copy(0.15f)))
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFEA4335), RoundedCornerShape(1.dp)))
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            TextField(
                                value = fromLocation,
                                onValueChange = { 
                                    onFromChange(it)
                                    focusedField = 0
                                },
                                placeholder = { Text("Pick-up location", color = Color.White.copy(0.4f)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = AppAccent
                                ),
                                singleLine = true
                            )
                            
                            additionalStops.forEachIndexed { index, stop ->
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        stop.name, 
                                        color = Color.White, 
                                        modifier = Modifier.weight(1f).padding(16.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(onClick = { onRemoveStop(index) }) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove stop", tint = Color.White.copy(0.4f))
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextField(
                                    value = toLocation,
                                    onValueChange = {
                                        onToChange(it)
                                        focusedField = 1
                                    },
                                    placeholder = { Text("Where to?", color = Color.White.copy(0.4f)) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        cursorColor = AppAccent
                                    ),
                                    singleLine = true
                                )
                                if (additionalStops.size < 2) { // Limit stops for simulation
                                    IconButton(onClick = { 
                                        // Simple logic: if toLocation is a valid destination name from suggestions
                                        // we could add it. For now, let's just use the first suggestion if available
                                        viewModel.suggestions.value.firstOrNull()?.let { 
                                            onAddStop(it)
                                            onToChange("")
                                        }
                                    }) {
                                        Icon(Icons.Default.Add, contentDescription = "Add stop", tint = AppAccent)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = isSearching)
        ) {
            if (isSearching) {
                if (isSearchingAddress) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = AppAccent,
                            trackColor = Color.Transparent
                        )
                    }
                }

                items(suggestions) { suggestion ->
                    DestinationItem(
                        destination = suggestion,
                        onFavoriteToggle = {
                            viewModel.saveDestination(it.name, it.address, it.latitude, it.longitude, !it.isFavorite, it.type.name.lowercase())
                        }
                    ) {
                        if (focusedField == 0) {
                            onPickupSelected(it)
                        } else {
                            onDestinationSelected(it)
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "Recent Destinations",
                        color = AppTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                items(recentItems) { destination ->
                    DestinationItem(
                        destination = destination,
                        onFavoriteToggle = {
                            viewModel.saveDestination(it.name, it.address, it.latitude, it.longitude, !it.isFavorite, it.type.name.lowercase())
                        }
                    ) { onDestinationSelected(it) }
                }
            }
        }
    }
}

@Composable
fun QuickActionChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFF233138),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color.White.copy(0.7f), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun DestinationItem(destination: Destination, onFavoriteToggle: (Destination) -> Unit = {}, onClick: (Destination) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(destination) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(destination.iconColor.copy(0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (destination.name == "Home") Icons.Default.Home else if (destination.name == "Work") Icons.Default.Work else Icons.Default.LocationOn,
                contentDescription = null,
                tint = destination.iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(destination.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(destination.address, color = AppTextSecondary, fontSize = 13.sp)
        }
        
        IconButton(onClick = { onFavoriteToggle(destination) }) {
            Icon(
                if (destination.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Favorite",
                tint = if (destination.isFavorite) NiceOrange else Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideSelectionFlow(
    destination: Destination?,
    selectedRide: RideOption?,
    onRideSelect: (RideOption) -> Unit,
    onBook: () -> Unit,
    onScheduleClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DriveViewModel
) {
    val options = viewModel.rideOptions.value
    val distance = viewModel.routeDistance.value
    val duration = viewModel.routeDuration.value
    val preferences = viewModel.ridePreferences
    val outOfServiceArea = viewModel.outOfServiceArea.value

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    if (outOfServiceArea) "Unserviceable Area" else "Choose a ride", 
                    color = if (outOfServiceArea) Color(0xFFEA4335) else Color.White, 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 24.sp
                )
                if (distance != null && duration != null && !outOfServiceArea) {
                    Text("$distance • $duration", color = AppTextSecondary, fontSize = 14.sp)
                }
            }
            Text("Personal", color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        
        if (outOfServiceArea) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOff, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("This destination is outside our service area.", color = AppTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Ride Preferences
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val prefList = preferences.keys.toList()
                items(prefList) { pref ->
                    val isSelected = preferences[pref] ?: false
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.togglePreference(pref) },
                        label = { Text(pref) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppAccent.copy(0.2f),
                            selectedLabelColor = AppAccent,
                            containerColor = Color.Transparent,
                            labelColor = Color.White.copy(0.7f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color.White.copy(0.2f),
                            selectedBorderColor = AppAccent,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options) { option ->
                    RideOptionItem(
                        option = option,
                        isSelected = selectedRide?.id == option.id,
                        onSelect = { onRideSelect(option) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = onScheduleClick,
                modifier = Modifier
                    .size(56.dp)
                    .background(AppSecondary, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = "Schedule", tint = Color.White)
            }
            
            Button(
                onClick = { if (!outOfServiceArea) onBook() else viewModel.clearRoute() },
                enabled = selectedRide != null || outOfServiceArea,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (outOfServiceArea) AppSecondary else AppAccent,
                    contentColor = if (outOfServiceArea) Color.White else AppBackground,
                    disabledContainerColor = AppAccent.copy(0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (outOfServiceArea) "Try another location" else if (selectedRide != null) "Request ${selectedRide.name}" else "Select a ride",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun RideOptionItem(option: RideOption, isSelected: Boolean, onSelect: () -> Unit) {
    Surface(
        onClick = onSelect,
        color = if (isSelected) AppAccent.copy(0.1f) else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, AppAccent) else null,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(option.iconColor.copy(0.15f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DirectionsCar, 
                    contentDescription = null, 
                    tint = option.iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(option.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Person, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(14.dp))
                    Text(" 4", color = AppTextSecondary, fontSize = 12.sp)
                }
                Text(option.arrivalTime, color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(option.description, color = AppTextSecondary, fontSize = 12.sp, maxLines = 1)
            }
            Text("R${option.price}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        }
    }
}

@Composable
fun LiveTrackingFlow(
    onCancel: () -> Unit,
    onShowDriverProfile: () -> Unit,
    viewModel: DriveViewModel,
    chatViewModel: com.example.firstapp.chat.ui.viewmodel.ChatViewModel
) {
    val context = LocalContext.current
    val assignedDriver = viewModel.assignedDriver.value
    val status = viewModel.bookingStatus.value
    var showRatingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (status == DriveViewModel.BookingStatus.COMPLETED) {
            showRatingDialog = true
        }
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = if (status == DriveViewModel.BookingStatus.IN_PROGRESS) NiceOrange else AppAccent,
            trackColor = Color.White.copy(0.1f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onShowDriverProfile() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (assignedDriver != null) AppAccent.copy(0.1f) else Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                if (assignedDriver != null) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = AppAccent, modifier = Modifier.size(32.dp))
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when(status) {
                        DriveViewModel.BookingStatus.SEARCHING -> "Finding your driver..."
                        DriveViewModel.BookingStatus.EN_ROUTE -> "Driver on the way"
                        DriveViewModel.BookingStatus.ARRIVED -> "Driver has arrived"
                        DriveViewModel.BookingStatus.IN_PROGRESS -> "Heading to destination"
                        DriveViewModel.BookingStatus.COMPLETED -> "Arrived at destination"
                        else -> "Processing..."
                    },
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp
                )
                val eta = viewModel.remainingTime.value
                Text(
                    text = if (assignedDriver == null) "Searching for nearby cars" else "${assignedDriver.name} • ${assignedDriver.vehicleModel}${if (eta != null) " • $eta" else ""}",
                    color = AppTextSecondary, 
                    fontSize = 14.sp
                )
                if (assignedDriver != null) {
                    Text(
                        text = assignedDriver.licensePlate,
                        color = Color.White.copy(0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (status != DriveViewModel.BookingStatus.IN_PROGRESS && status != DriveViewModel.BookingStatus.COMPLETED) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }
            } else if (status == DriveViewModel.BookingStatus.IN_PROGRESS) {
                // SOS Button during trip
                Button(
                    onClick = {
                        try {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SOS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        
        if (assignedDriver != null && status != DriveViewModel.BookingStatus.COMPLETED) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            val driverId = assignedDriver.id.replace("driver_", "")
                            chatViewModel.initiateCall(driverId, com.example.firstapp.chat.data.CallType.VOICE)
                        },
                        modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = Color.White)
                    }
                    Text("Call", color = Color.White, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            val driverId = assignedDriver.id.replace("driver_", "")
                            chatViewModel.initiateCall(driverId, com.example.firstapp.chat.data.CallType.VIDEO)
                        },
                        modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.VideoCall, contentDescription = "Video Call", tint = Color.White)
                    }
                    Text("Video", color = Color.White, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:+27100000000"))
                            smsIntent.putExtra("sms_body", "Hi, I am at the pickup point.")
                            context.startActivity(smsIntent)
                        },
                        modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message", tint = Color.White)
                    }
                    Text("Message", color = Color.White, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "I am currently on a FirstApp Drive trip. My ETA is ${viewModel.remainingTime.value ?: "soon"}.")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share trip"))
                            Toast.makeText(context, "Trip details ready to share", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                    Text("Share", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            color = Color(0xFF233138),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (status == DriveViewModel.BookingStatus.IN_PROGRESS) Icons.Default.Lock else Icons.Default.Shield, 
                    contentDescription = null, 
                    tint = AppAccent
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (status == DriveViewModel.BookingStatus.IN_PROGRESS) "Ride is PIN protected" else "Your safety is our priority", 
                        color = Color.White, 
                        fontSize = 14.sp
                    )
                    if (status != DriveViewModel.BookingStatus.IN_PROGRESS && status != DriveViewModel.BookingStatus.COMPLETED) {
                        viewModel.safetyPin.value?.let { pin ->
                            Text(
                                text = "Verify with PIN: $pin",
                                color = AppAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRatingDialog) {
        RatingDialog(
            driverName = "Mpho",
            onDismiss = { 
                showRatingDialog = false
                onCancel() // Return to search
            }
        )
    }
}

@Composable
fun RatingDialog(driverName: String, onDismiss: () -> Unit) {
    var rating by remember { mutableIntStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(AppAccent.copy(0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = AppAccent, modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("How was your trip with $driverName?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        },
        text = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(5) { index ->
                    IconButton(onClick = { rating = index + 1 }) {
                        Icon(
                            imageVector = if (index < rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (index < rating) NiceOrange else Color.White.copy(0.3f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                enabled = rating > 0,
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Submit Rating", color = AppBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip", color = AppTextSecondary)
            }
        }
    )
}

@Composable
fun ScheduleRideDialog(onDismiss: () -> Unit, onSchedule: (String, String) -> Unit) {
    var selectedDate by remember { mutableStateOf("Tomorrow") }
    var selectedTime by remember { mutableStateOf("10:00 AM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSecondary,
        title = { Text("Schedule a Ride", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Select pickup time for your future trip.", color = AppTextSecondary)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val dates = listOf("Today", "Tomorrow", "18 Oct", "19 Oct")
                    dates.forEach { date ->
                        val isSelected = selectedDate == date
                        Surface(
                            onClick = { selectedDate = date },
                            color = if (isSelected) AppAccent else Color.White.copy(0.05f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(date, color = if (isSelected) AppBackground else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val times = listOf("08:00 AM", "10:00 AM", "12:00 PM", "02:00 PM")
                    times.forEach { time ->
                        val isSelected = selectedTime == time
                        Surface(
                            onClick = { selectedTime = time },
                            color = if (isSelected) AppAccent else Color.White.copy(0.05f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(time, color = if (isSelected) AppBackground else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSchedule(selectedDate, selectedTime) },
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
            ) {
                Text("Schedule", color = AppBackground, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTextSecondary)
            }
        }
    )
}
