package com.example.firstapp.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.model.Destination
import com.example.firstapp.model.RideOption
import com.example.firstapp.ui.theme.*
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriveScreen(
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    viewModel: DriveViewModel = viewModel()
) {
    val context = LocalContext.current
    var showHistory by remember { mutableStateOf(false) }
    
    if (showHistory) {
        DriveHistoryScreen(onBack = { showHistory = false })
        return
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
    var selectedDestination by remember { mutableStateOf<Destination?>(null) }
    var selectedRide by remember { mutableStateOf<RideOption?>(null) }
    var fromLocation by remember { mutableStateOf("Current Location") }
    var toLocation by remember { mutableStateOf("") }
    
    val pickupAddress by viewModel.pickupAddress
    LaunchedEffect(pickupAddress) {
        fromLocation = pickupAddress
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = { 
            DriveDrawerContent(onShowHistory = { 
                showHistory = true 
                scope.launch { drawerState.close() }
            }) 
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
                        viewModel.calculateRoute(pickup, LatLng(detailedDest.latitude, detailedDest.longitude))
                    }
                } else {
                    selectedDestination = null
                }
            },
            selectedRide = selectedRide,
            onRideSelect = { selectedRide = it },
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
            bottomPadding = bottomPadding,
            viewModel = viewModel
        )
    }
}

@Composable
fun DriveDrawerContent(onShowHistory: () -> Unit) {
    val context = LocalContext.current
    ModalDrawerSheet(
        drawerContainerColor = AppSecondary,
        modifier = Modifier.width(320.dp),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AppAccent.copy(0.1f), Color.Transparent)))
                .padding(24.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(AppAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("JD", color = AppBackground, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("John Doe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    Text(" 4.98 • Gold Member", color = AppTextSecondary, fontSize = 14.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        DrawerItem(Icons.AutoMirrored.Filled.List, "My Rides") { onShowHistory() }
        DrawerItem(Icons.Default.Payment, "Payment Methods") { Toast.makeText(context, "Payment methods coming soon", Toast.LENGTH_SHORT).show() }
        DrawerItem(Icons.Default.CardGiftcard, "Promotions") { Toast.makeText(context, "Promotions coming soon", Toast.LENGTH_SHORT).show() }
        DrawerItem(Icons.Default.SupportAgent, "Support") { Toast.makeText(context, "Support coming soon", Toast.LENGTH_SHORT).show() }
        DrawerItem(Icons.Default.Info, "About FirstApp") { Toast.makeText(context, "FirstApp Drive v1", Toast.LENGTH_SHORT).show() }
        
        Spacer(modifier = Modifier.weight(1f))
        
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 24.dp))
        DrawerItem(Icons.Default.Settings, "Settings") { Toast.makeText(context, "Settings coming soon", Toast.LENGTH_SHORT).show() }
        Spacer(modifier = Modifier.height(24.dp))
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
    selectedRide: RideOption?,
    onRideSelect: (RideOption?) -> Unit,
    fromLocation: String,
    onFromLocationChange: (String) -> Unit,
    toLocation: String,
    onToLocationChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    viewModel: DriveViewModel
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

    val routePoints = viewModel.routePoints.value

    val currentPeekHeight = remember(step, isSearchingView, bottomPadding) {
        when {
            step == 2 -> 360.dp + bottomPadding
            else -> 140.dp + bottomPadding
        }
    }

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
                                    viewModel.calculateRoute(LatLng(detailed.latitude, detailed.longitude), LatLng(it.latitude, it.longitude))
                                }
                            }
                        },
                        modifier = if (isSearchingView) Modifier.fillMaxSize() else Modifier.wrapContentHeight(),
                        viewModel = viewModel
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
                        viewModel = viewModel
                    )
                }
            }
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
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
                            anchor = Offset(0.5f, 0.5f)
                        )
                    }
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
                            zIndex = 10f
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

                    if (routePoints.isNotEmpty()) {
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
            
            // Map overlay buttons
            Column(
                modifier = Modifier
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
                        onClick = { Toast.makeText(context, "Safety center opened", Toast.LENGTH_SHORT).show() },
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
        }
    }
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
    paint.color = android.graphics.Color.parseColor("#32D74B") // Bolt Green
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
    focusRequester: FocusRequester,
    onDestinationSelected: (Destination) -> Unit,
    onPickupSelected: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DriveViewModel
) {
    val context = LocalContext.current
    val recentItems = remember {
        listOf(
            Destination(1, "Home", "123 Innovation Drive, Sandton", iconColor = Color(0xFF4CAF50), latitude = -26.1265, longitude = 28.0341),
            Destination(2, "Work", "Tech Park Building, Midrand", iconColor = Color(0xFF2196F3), latitude = -25.9876, longitude = 28.1234),
            Destination(3, "Gym", "Virgin Active, Alice Ln", iconColor = Color(0xFFFF9800), latitude = -26.1023, longitude = 28.0512)
        )
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
                "Good morning, John",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionChip(Icons.Default.Home, "Home") { 
                    onDestinationSelected(Destination(1, "Home", "123 Innovation Drive, Sandton", iconColor = Color(0xFF4CAF50), latitude = -26.1265, longitude = 28.0341)) 
                }
                QuickActionChip(Icons.Default.Work, "Work") { 
                    onDestinationSelected(Destination(2, "Work", "Tech Park Building, Midrand", iconColor = Color(0xFF2196F3), latitude = -25.9876, longitude = 28.1234))
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
                            Box(modifier = Modifier.width(1.dp).height(50.dp).background(Color.White.copy(0.15f)))
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
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                            TextField(
                                value = toLocation,
                                onValueChange = {
                                    onToChange(it)
                                    focusedField = 1
                                },
                                placeholder = { Text("Where to?", color = Color.White.copy(0.4f)) },
                                modifier = Modifier
                                    .fillMaxWidth()
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
                    DestinationItem(suggestion) {
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
                    DestinationItem(destination) { onDestinationSelected(it) }
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
fun DestinationItem(destination: Destination, onClick: (Destination) -> Unit) {
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
        Column {
            Text(destination.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(destination.address, color = AppTextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
fun RideSelectionFlow(
    destination: Destination?,
    selectedRide: RideOption?,
    onRideSelect: (RideOption) -> Unit,
    onBook: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DriveViewModel
) {
    val options = viewModel.rideOptions.value
    val distance = viewModel.routeDistance.value
    val duration = viewModel.routeDuration.value

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text("Choose a ride", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                if (distance != null && duration != null) {
                    Text("$distance • $duration", color = AppTextSecondary, fontSize = 14.sp)
                }
            }
            Text("Personal", color = AppAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onBook,
            enabled = selectedRide != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppAccent,
                contentColor = AppBackground,
                disabledContainerColor = AppAccent.copy(0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (selectedRide != null) "Request ${selectedRide.name}" else "Select a ride",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
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
    viewModel: DriveViewModel
) {
    val context = LocalContext.current
    val assignedDriver = viewModel.assignedDriver.value
    
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = AppAccent,
            trackColor = Color.White.copy(0.1f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                if (assignedDriver != null) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (assignedDriver == null) "Finding your driver..." else "Driver on the way", 
                    color = Color.White, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp
                )
                Text(
                    if (assignedDriver == null) "Searching for nearby cars" else "FirstApp Go • BMW 3 Series", 
                    color = AppTextSecondary, 
                    fontSize = 14.sp
                )
            }
            IconButton(
                onClick = onCancel,
                modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
            }
        }
        
        if (assignedDriver != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+27100000000"))
                            context.startActivity(dialIntent)
                        },
                        modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                    }
                    Text("Call", color = Color.White, fontSize = 12.sp)
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
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = Color.White)
                    }
                    Text("Message", color = Color.White, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "I am currently on a FirstApp Drive trip.")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share trip"))
                        },
                        modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
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
                Icon(Icons.Default.Shield, contentDescription = null, tint = AppAccent)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Your safety is our priority", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
