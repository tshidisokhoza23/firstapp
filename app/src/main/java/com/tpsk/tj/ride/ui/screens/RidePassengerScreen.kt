package com.tpsk.tj.ride.ui.screens

import com.tpsk.tj.drive.ui.screens.CancellationDialog

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.tpsk.tj.drive.model.BookingStatus
import com.tpsk.tj.drive.model.Destination
import com.tpsk.tj.drive.model.DestinationType
import com.tpsk.tj.drive.model.RideOption
import com.tpsk.tj.drive.ui.viewmodel.DriveViewModel
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RidePassengerScreen(
    viewModel: DriveViewModel,
    userName: String,
    onMenuClick: () -> Unit,
    chatViewModel: com.tpsk.tj.chat.ui.viewmodel.ChatViewModel,
    bottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val status = viewModel.bookingStatus.value
    val savedPlaces by viewModel.destinations.collectAsState()
    var editing by remember { mutableStateOf<String?>(null) }
    var showCancel by remember { mutableStateOf(false) }
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TJ Ride",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (editing != null) editing = null else onMenuClick()
                    }) {
                        Icon(
                            if (editing != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                            contentDescription = if (editing != null) "Back" else "Menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                editing != null -> RidePlaceSearch(
                    title = when (editing) {
                        "pickup" -> "Pickup"
                        "stop" -> "Add stop"
                        else -> "Destination"
                    },
                    viewModel = viewModel,
                    onPick = { place ->
                        viewModel.getPlaceDetails(context, place) { detailed ->
                            if (editing == "pickup") {
                                viewModel.updatePickupLocation(
                                    LatLng(detailed.latitude, detailed.longitude),
                                    detailed.name.ifBlank { detailed.address }
                                )
                            } else if (editing == "stop") {
                                viewModel.addTripStop(detailed)
                            } else {
                                viewModel.updateSelectedDestination(detailed)
                                viewModel.refreshQuotedRoute()
                            }
                            editing = null
                        }
                    }
                )
                status == BookingStatus.SEARCHING -> RideRequestingCard(
                    pickup = viewModel.pickupAddress.value,
                    destination = viewModel.selectedDestination.value?.name ?: "Destination",
                    ride = viewModel.selectedRide.value,
                    onCancel = { showCancel = true },
                    modifier = Modifier.padding(bottom = bottomPadding)
                )
                status == BookingStatus.EN_ROUTE || status == BookingStatus.ARRIVED || status == BookingStatus.IN_PROGRESS -> RideActiveCard(
                    viewModel = viewModel,
                    chatViewModel = chatViewModel,
                    onCancel = { showCancel = true },
                    modifier = Modifier.padding(bottom = bottomPadding)
                )
                else -> RideHomeCard(
                    greeting = "$greeting, $userName",
                    pickup = viewModel.pickupAddress.value.ifBlank { "Current location" },
                    destination = viewModel.selectedDestination.value?.name ?: "",
                    options = viewModel.rideOptions.value,
                    selected = viewModel.selectedRide.value,
                    places = savedPlaces.data.orEmpty(),
                    onEditPickup = { editing = "pickup" },
                    onEditDestination = { editing = "dest" },
                    onAddStop = { editing = "stop" },
                    onRemoveStop = { viewModel.removeTripStop(it) },
                    stops = viewModel.tripStops,
                    area = viewModel.areaPresence.value,
                    areaError = viewModel.areaError.value,
                    routePoints = viewModel.routePoints.value,
                    onPlace = { place ->
                        viewModel.updateSelectedDestination(place)
                        viewModel.refreshQuotedRoute()
                    },
                    onSelectRide = { viewModel.selectRide(it) },
                    onRequest = {
                        val dest = viewModel.selectedDestination.value
                        val ride = viewModel.selectedRide.value ?: viewModel.rideOptions.value.firstOrNull()
                        if (dest == null) {
                            editing = "dest"
                        } else if (ride == null) {
                            Toast.makeText(context, "Choose a vehicle first", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.selectRide(ride)
                            viewModel.startBooking()
                        }
                    },
                    modifier = Modifier.padding(bottom = bottomPadding)
                )
            }
        }
    }

    if (showCancel) {
        CancellationDialog(
            onDismiss = { showCancel = false },
            onConfirm = { reason ->
                viewModel.cancelRide(reason, "PASSENGER")
                showCancel = false
            }
        )
    }
}

@Composable
private fun RideHomeCard(
    greeting: String,
    pickup: String,
    destination: String,
    options: List<RideOption>,
    selected: RideOption?,
    places: List<Destination>,
    onEditPickup: () -> Unit,
    onEditDestination: () -> Unit,
    onAddStop: () -> Unit,
    onRemoveStop: (Int) -> Unit,
    stops: List<Destination>,
    area: com.tpsk.tj.drive.model.AreaPresence?,
    areaError: String?,
    routePoints: List<LatLng>,
    onPlace: (Destination) -> Unit,
    onSelectRide: (RideOption) -> Unit,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val home = places.find { it.type == DestinationType.HOME }
    val work = places.find { it.type == DestinationType.WORK }
    val popular = places.filter { it.type != DestinationType.HOME && it.type != DestinationType.WORK }.take(4)
    val chosen = selected ?: options.firstOrNull()
    LaunchedEffect(options) {
        if (selected == null && options.isNotEmpty()) onSelectRide(options.first())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text(greeting, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Where are you heading?",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp
        )
        Spacer(Modifier.height(12.dp))
        AreaCountCard(area, areaError)
        Spacer(Modifier.height(16.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RideStopRow("Pickup", pickup.ifBlank { "Current location" }, MaterialTheme.colorScheme.primary, onEditPickup)
                stops.forEachIndexed { index, stop ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            RideStopRow("Stop ${index + 1}", stop.name.ifBlank { stop.address }, MaterialTheme.colorScheme.tertiary, {})
                        }
                        Text("Remove", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.clickable { onRemoveStop(index) })
                    }
                }
                RideStopRow("Destination", destination.ifBlank { "Where to?" }, MaterialTheme.colorScheme.error, onEditDestination)
                if (stops.size < 2) {
                    Text("Add stop", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAddStop))
                }
            }
        }
        if (routePoints.size > 1) {
            Spacer(Modifier.height(12.dp))
            LiveRouteMap(routePoints)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RideShortcut(Icons.Default.Home, home?.name ?: "Home", Modifier.weight(1f)) {
                if (home != null) onPlace(home) else onEditDestination()
            }
            RideShortcut(Icons.Default.Work, work?.name ?: "Work", Modifier.weight(1f)) {
                if (work != null) onPlace(work) else onEditDestination()
            }
        }
        if (popular.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Text("Popular nearby", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            popular.forEach { place ->
                Surface(
                    onClick = { onPlace(place) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(place.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (place.address.isNotBlank()) {
                                Text(place.address, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Select vehicle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        if (options.isEmpty()) {
            Text(
                "Set a destination to see prices.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        } else {
            options.forEach { option ->
                val isSelected = chosen?.id == option.id
                Surface(
                    onClick = { onSelectRide(option) },
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${option.name} (${option.capacity} seats)", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${option.arrivalTime.ifBlank { "Checking drivers" }} · live traffic · Wallet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        Text("R ${option.price}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Price includes the live traffic time. Waiting is free for 3 minutes, then R2 per minute.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("REQUEST RIDE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RideStopRow(label: String, value: String, dot: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RideShortcut(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RidePlaceSearch(
    title: String,
    viewModel: DriveViewModel,
    onPick: (Destination) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val suggestions = viewModel.suggestions.value
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.searchPlaces(context, it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search a place") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions) { place ->
                Surface(
                    onClick = { onPick(place) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(place.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (place.address.isNotBlank()) {
                            Text(place.address, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RideRequestingCard(
    pickup: String,
    destination: String,
    ride: RideOption?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Requesting ride", fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Request sent", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Finding a free driver...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "You will see it here as soon as a driver accepts. A driver who is already completing a ride is not offered this request.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RideStopRow("Pickup", pickup.ifBlank { "Current location" }, MaterialTheme.colorScheme.primary) {}
                RideStopRow("Destination", destination, MaterialTheme.colorScheme.error) {}
            }
        }
        if (ride != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("${ride.name} · R ${ride.price}", fontWeight = FontWeight.SemiBold)
                        Text("Wallet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("CANCEL REQUEST", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RideActiveCard(
    viewModel: DriveViewModel,
    chatViewModel: com.tpsk.tj.chat.ui.viewmodel.ChatViewModel,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driver = viewModel.assignedDriver.value
    val status = viewModel.bookingStatus.value
    val eta = viewModel.remainingTime.value
    var nowSeconds by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(status) {
        while (true) {
            nowSeconds = System.currentTimeMillis() / 1000
            delay(1000)
        }
    }
    val title = when (status) {
        BookingStatus.ARRIVED -> "Driver has arrived"
        BookingStatus.IN_PROGRESS -> "Your driver is completing this ride"
        else -> if (eta.isNullOrBlank()) "Driver accepted. They are on the way" else "Driver accepted · arriving in $eta"
    }
    val subtitle = when (status) {
        BookingStatus.ARRIVED -> "Meet your driver and give them the PIN to start. ${waitCopy(viewModel.arrivedAt.value, nowSeconds)}"
        BookingStatus.IN_PROGRESS -> "The trip has started. You will see it here until the driver finishes."
        else -> "The driver accepted your request and is coming to the pickup."
    }
    val progress = when (status) {
        BookingStatus.ARRIVED -> 0.7f
        BookingStatus.IN_PROGRESS -> 0.9f
        else -> 0.4f
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        viewModel.activeStops.value.forEach { stop ->
            val state = when {
                stop.left_at > 0L -> "done"
                stop.arrived_at > 0L -> "waiting, R2 per minute after 3 free minutes"
                else -> "coming up"
            }
            Text("${stop.address.ifBlank { "Stop ${stop.index + 1}" }} · $state", fontSize = 13.sp)
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(driver?.name?.take(1)?.uppercase() ?: "D", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(driver?.name ?: "Your driver", fontWeight = FontWeight.Bold)
                        if (driver != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Text(" ${"%.1f".format(driver.rating)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(driver?.vehicleModel ?: "Vehicle", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(driver?.licensePlate ?: "", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val id = driver?.id?.replace("driver_", "") ?: return@Button
                            chatViewModel.startVoiceOrVideoCall(
                                receiverId = id,
                                callType = com.tpsk.tj.chat.data.CallType.VOICE,
                                name = driver.name
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Call")
                    }
                    Button(
                        onClick = {
                            val id = driver?.id?.replace("driver_", "") ?: return@Button
                            scope.launch {
                                chatViewModel.initiateDM(id).collect { res ->
                                    if (res is com.tpsk.tj.common.util.Resource.Success && !res.data.isNullOrBlank()) {
                                        Toast.makeText(context, "Chat opened", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Message")
                    }
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Trip details", fontWeight = FontWeight.Bold)
                RideStopRow("Pickup", viewModel.pickupAddress.value.ifBlank { "Pickup" }, MaterialTheme.colorScheme.primary) {}
                RideStopRow("Dropoff", viewModel.selectedDestination.value?.name ?: "Destination", MaterialTheme.colorScheme.error) {}
                Text(
                    "Fare  R ${"%.2f".format(viewModel.selectedRide.value?.price?.toDoubleOrNull() ?: 0.0)} (Wallet)",
                    fontWeight = FontWeight.SemiBold
                )
                viewModel.safetyPin.value?.let { pin ->
                    Text("Your PIN is $pin. Give it to the driver to start.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (status != BookingStatus.IN_PROGRESS) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("EMERGENCY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("CANCEL RIDE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun AreaCountCard(area: com.tpsk.tj.drive.model.AreaPresence?, error: String?) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                when {
                    area != null -> "${area.drivers} drivers around you within ${area.radius_km.toInt()} km"
                    error != null -> "Couldn't check drivers nearby"
                    else -> "Checking drivers around you"
                },
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Online drivers in this area. Names and pins stay hidden.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun LiveRouteMap(points: List<LatLng>) {
    val camera = rememberCameraPositionState()
    LaunchedEffect(points) {
        if (points.size < 2) return@LaunchedEffect
        val bounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
        runCatching { camera.move(CameraUpdateFactory.newLatLngBounds(bounds, 48)) }
    }
    GoogleMap(
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)),
        cameraPositionState = camera,
        properties = MapProperties(isTrafficEnabled = true),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            scrollGesturesEnabled = false,
            zoomGesturesEnabled = false,
            myLocationButtonEnabled = false
        )
    ) {
        Polyline(points = points, color = MaterialTheme.colorScheme.primary, width = 10f)
    }
}

private fun waitCopy(arrivedAt: Long, nowSeconds: Long): String {
    if (arrivedAt <= 0L) return "3 minutes are free, then waiting is R2 per minute."
    val left = 180 - (nowSeconds - arrivedAt)
    return if (left > 0) "Free waiting: ${left / 60}m ${left % 60}s left" else "Waiting fee is running at R2 per minute"
}
