@file:Suppress("DEPRECATION")

package com.example.firstapp.drive.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.drive.domain.repository.DriveRepository
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.model.*
import com.example.firstapp.drive.model.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.drive.data.network.RideEvent
import com.example.firstapp.drive.data.service.DriverLocationService
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



@HiltViewModel
class DriveViewModel @Inject constructor(
    private val repository: DriveRepository
) : ViewModel() {

    private val _destinations = MutableStateFlow<Resource<List<Destination>>>(Resource.Loading())
    val destinations: StateFlow<Resource<List<Destination>>> = _destinations.asStateFlow()

    private val _trips = MutableStateFlow<Resource<List<Trip>>>(Resource.Loading())
    val trips: StateFlow<Resource<List<Trip>>> = _trips.asStateFlow()

    private val _suggestions = mutableStateOf<List<Destination>>(emptyList())
    val suggestions: State<List<Destination>> = _suggestions

    private val _pickupLocation = mutableStateOf<LatLng?>(null)
    val pickupLocation: State<LatLng?> = _pickupLocation

    private val _pickupAddress = mutableStateOf("Current Location")
    val pickupAddress: State<String> = _pickupAddress

    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    private val _routePoints = mutableStateOf<List<LatLng>>(emptyList())
    val routePoints: State<List<LatLng>> = _routePoints

    private val _trafficSegments = mutableStateOf<List<TrafficSegment>>(emptyList())
    val trafficSegments: State<List<TrafficSegment>> = _trafficSegments

    private val _routeDistance = mutableStateOf<String?>(null)
    val routeDistance: State<String?> = _routeDistance

    private val _routeDuration = mutableStateOf<String?>(null)
    val routeDuration: State<String?> = _routeDuration

    private val _outOfServiceArea = mutableStateOf(false)
    val outOfServiceArea: State<Boolean> = _outOfServiceArea

    private val _rideOptions = mutableStateOf<List<RideOption>>(emptyList())
    val rideOptions: State<List<RideOption>> = _rideOptions

    private val _selectedRide = mutableStateOf<RideOption?>(null)
    val selectedRide: State<RideOption?> = _selectedRide

    private val _selectedDestination = mutableStateOf<Destination?>(null)
    val selectedDestination: State<Destination?> = _selectedDestination

    private val _nearbyDrivers = mutableStateOf<List<NearbyDriver>>(emptyList())
    val nearbyDrivers: State<List<NearbyDriver>> = _nearbyDrivers

    private val _assignedDriver = mutableStateOf<NearbyDriver?>(null)
    val assignedDriver: State<NearbyDriver?> = _assignedDriver

    private val _bookingStatus = mutableStateOf<BookingStatus>(BookingStatus.NONE)
    val bookingStatus: State<BookingStatus> = _bookingStatus

    private val _remainingTime = mutableStateOf<String?>(null)
    val remainingTime: State<String?> = _remainingTime

    private val _safetyPin = mutableStateOf<String?>(null)
    val safetyPin: State<String?> = _safetyPin

    private val _walletBalance = mutableStateOf(245.50)
    val walletBalance: State<Double> = _walletBalance

    private val _transactions = mutableStateListOf<Transaction>(
        Transaction("1", "Trip to Tech Park", -85.50, "15 Oct", true),
        Transaction("2", "Wallet Top Up", 200.00, "14 Oct", false),
        Transaction("3", "Trip to Mall", -120.00, "12 Oct", true)
    )
    val transactions: List<Transaction> = _transactions

    private val _ridePreferences = mutableStateMapOf<String, Boolean>(
        "Quiet trip" to false,
        "Cool aircon" to false,
        "Extra luggage" to false
    )
    val ridePreferences: Map<String, Boolean> = _ridePreferences

    // --- Driver Mode State ---
    private val _isDriverMode = MutableStateFlow(false)
    val isDriverMode: StateFlow<Boolean> = _isDriverMode.asStateFlow()

    private val _driverOnlineStatus = MutableStateFlow(false)
    val driverOnlineStatus: StateFlow<Boolean> = _driverOnlineStatus.asStateFlow()

    private val _driverDocuments = MutableStateFlow<List<DriverDocument>>(emptyList())
    val driverDocuments: StateFlow<List<DriverDocument>> = _driverDocuments.asStateFlow()

    private val _isDriverApproved = MutableStateFlow(false)
    val isDriverApproved: StateFlow<Boolean> = _isDriverApproved.asStateFlow()

    private val _incomingRideRequest = MutableStateFlow<RideRequest?>(null)
    val incomingRideRequest: StateFlow<RideRequest?> = _incomingRideRequest.asStateFlow()

    private val _driverEarnings = MutableStateFlow(0.0)
    val driverEarnings: StateFlow<Double> = _driverEarnings.asStateFlow()

    private val _driverStats = MutableStateFlow(DriverStats())
    val driverStats: StateFlow<DriverStats> = _driverStats.asStateFlow()

    private val _completedTripSummary = MutableStateFlow<TripSummary?>(null)
    val completedTripSummary: StateFlow<TripSummary?> = _completedTripSummary.asStateFlow()

    private val _driverPosition = MutableStateFlow<LatLng?>(null)
    val driverPosition: StateFlow<LatLng?> = _driverPosition.asStateFlow()

    private val _vehicleInfo = MutableStateFlow(VehicleInfo())
    val vehicleInfo: StateFlow<VehicleInfo> = _vehicleInfo.asStateFlow()

    private val _activeTripId = MutableStateFlow<String?>(null)
    val activeTripId: StateFlow<String?> = _activeTripId.asStateFlow()

    private var locationService: DriverLocationService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DriverLocationService.LocalBinder
            locationService = binder.getService()
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService = null
            isServiceBound = false
        }
    }

    private val _isDriveActive = MutableStateFlow(false)
    val isDriveActive: StateFlow<Boolean> = _isDriveActive.asStateFlow()

    fun setDriveActive(active: Boolean) {
        _isDriveActive.value = active
    }

    private val apiKey = "AIzaSyDvArA2Ykp3Q6EsQMVSre4piq9SvARF7H4"

    private var searchJob: Job? = null

    init {
        fetchDestinations()
        fetchTrips()
        fetchRideOptions()
        loadDriverState()
        observeRideEvents()
        loadActiveSession()
    }

    private fun observeRideEvents() {
        viewModelScope.launch {
            repository.getRideEvents().collect { event ->
                when (event) {
                    is RideEvent.RequestReceived -> {
                        if (_isDriverMode.value && _driverOnlineStatus.value) {
                            val myPos = _driverPosition.value ?: _pickupLocation.value
                            val distanceLabel = if (myPos != null) {
                                val results = FloatArray(1)
                                android.location.Location.distanceBetween(myPos.latitude, myPos.longitude, event.pickupLat, event.pickupLng, results)
                                String.format(Locale.getDefault(), "%.1f km", results[0] / 1000.0)
                            } else {
                                "New request"
                            }

                            _incomingRideRequest.value = RideRequest(
                                id = event.tripId,
                                passengerName = event.passengerName,
                                pickupAddress = event.pickup,
                                destinationAddress = event.dest,
                                fare = event.fare,
                                distance = distanceLabel,
                                pickupLat = event.pickupLat,
                                pickupLng = event.pickupLng,
                                destLat = event.destLat,
                                destLng = event.destLng
                            )
                        }
                    }
                    is RideEvent.RequestAccepted -> {
                        _bookingStatus.value = BookingStatus.EN_ROUTE
                        _assignedDriver.value = NearbyDriver(
                            id = "assigned",
                            position = LatLng(0.0, 0.0), // Will be updated by location event
                            rotation = 0f,
                            name = event.driverName,
                            vehicleModel = event.vehicle,
                            licensePlate = event.plate
                        )
                    }
                    is RideEvent.DriverArrived -> {
                        _bookingStatus.value = BookingStatus.ARRIVED
                        _remainingTime.value = "Arrived"
                    }
                    is RideEvent.TripStarted -> {
                        _bookingStatus.value = BookingStatus.IN_PROGRESS
                    }
                    is RideEvent.TripCompleted -> {
                        val currentDriver = _assignedDriver.value
                        _completedTripSummary.value = TripSummary(
                            fare = event.fare,
                            distance = _routeDistance.value ?: "0 km",
                            duration = _routeDuration.value ?: "0 min",
                            passengerName = currentDriver?.name ?: "Driver"
                        )
                        _bookingStatus.value = BookingStatus.COMPLETED
                        _assignedDriver.value = null
                        _activeTripId.value = null
                    }
                    is RideEvent.LocationUpdated -> {
                        _assignedDriver.value = _assignedDriver.value?.copy(
                            position = LatLng(event.lat, event.lng),
                            rotation = event.rotation
                        )
                    }
                }
            }
        }
    }

    private fun loadActiveSession() {
        viewModelScope.launch {
            repository.getActiveSession().collect { res ->
                if (res is Resource.Success && res.data != null) {
                    val session = res.data
                    _isDriverMode.value = session.mode == "DRIVER"
                    _driverOnlineStatus.value = session.is_online
                    _activeTripId.value = session.ride_id
                    _bookingStatus.value = session.status
                    _assignedDriver.value = session.assigned_driver
                    _selectedDestination.value = session.destination
                    
                    if (session.is_online && session.mode == "DRIVER") {
                        repository.connectRideSocket()
                        startLocationService()
                    } else if (session.status != BookingStatus.NONE && session.status != BookingStatus.COMPLETED) {
                        repository.connectRideSocket()
                    }
                }
            }
        }
    }

    private fun loadDriverState() {
        viewModelScope.launch {
            repository.getDriverDocuments().collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _driverDocuments.value = res.data
                    updateApprovalStatus()
                } else if (res is Resource.Error) {
                    _driverDocuments.value = emptyList()
                    _isDriverApproved.value = false
                }
            }
        }
    }

    private fun updateApprovalStatus() {
        val requiredTypes = listOf("ID Document", "Driver's License", "Criminal Verification", "Roadworthy Certificate", "Car Image")
        val approvedDocs = _driverDocuments.value.filter { it.status == "APPROVED" }.map { it.type }
        val allApproved = requiredTypes.all { it in approvedDocs }
        _isDriverApproved.value = allApproved
    }

    private val _isSwitchingMode = MutableStateFlow(false)
    val isSwitchingMode: StateFlow<Boolean> = _isSwitchingMode.asStateFlow()

    fun toggleDriverMode(enabled: Boolean) {
        // Hard Guard: Cannot switch modes while a trip is active
        if (_bookingStatus.value != BookingStatus.NONE && _bookingStatus.value != BookingStatus.COMPLETED) {
            return
        }

        viewModelScope.launch {
            _isSwitchingMode.value = true
            
            // Re-validate against backend
            repository.updateOnlineStatus(false, 0.0, 0.0).collect { } 
            delay(1000L) 

            if (!enabled && _driverOnlineStatus.value) {
                setDriverOnlineStatus(false)
            }

            _isDriverMode.value = enabled
            if (enabled) {
                repository.connectRideSocket()
            } else {
                repository.disconnectRideSocket()
                stopLocationService()
            }
            
            _isSwitchingMode.value = false
        }
    }

    fun setDriverOnlineStatus(online: Boolean) {
        _driverOnlineStatus.value = online
        val pos = _driverPosition.value ?: _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
        
        viewModelScope.launch {
            repository.updateOnlineStatus(online, pos.latitude, pos.longitude).collect { }
            if (online) {
                repository.connectRideSocket()
                startLocationService()
            } else {
                stopLocationService()
                _incomingRideRequest.value = null
            }
        }
    }

    private fun startLocationService() {
        val context = com.example.firstapp.common.util.ContextProvider.context
        val intent = Intent(context, DriverLocationService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopLocationService() {
        val context = com.example.firstapp.common.util.ContextProvider.context
        if (isServiceBound) {
            context.unbindService(serviceConnection)
            isServiceBound = false
        }
        val intent = Intent(context, DriverLocationService::class.java).apply {
            action = DriverLocationService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
    }



    fun acceptRideRequest() {
        viewModelScope.launch {
            val req = _incomingRideRequest.value ?: return@launch
            repository.updateRideStatus(req.id, "ACCEPTED").collect { res ->
                if (res is Resource.Success) {
                    _incomingRideRequest.value = null
                    _bookingStatus.value = BookingStatus.EN_ROUTE
                }
            }
        }
    }

    fun verifySafetyPin(pin: String): Boolean {
        return if (pin == _safetyPin.value) {
            _bookingStatus.value = BookingStatus.IN_PROGRESS
            true
        } else {
            false
        }
    }

    fun requestPayout() {
        viewModelScope.launch {
            // Logic to transfer _driverEarnings to _walletBalance
            val amount = _driverEarnings.value
            if (amount > 0) {
                _walletBalance.value += amount
                _driverEarnings.value = 0.0
            }
        }
    }

    fun addFunds(amount: Double) {
        _walletBalance.value += amount
        _transactions.add(0, Transaction(System.currentTimeMillis().toString(), "Wallet Top Up", amount, "Just now", false))
    }

    fun declineRideRequest() {
        _incomingRideRequest.value = null
    }

    fun cancelRide(reason: String, by: String) {
        val rideId = "simulated_id" // Ideally track the active ride ID
        viewModelScope.launch {
            repository.cancelRide(rideId, reason, by).collect { res ->
                if (res is Resource.Success) {
                    _bookingStatus.value = BookingStatus.NONE
                    _assignedDriver.value = null
                    clearRoute()
                }
            }
        }
    }

    fun updateVehicleInfo(info: VehicleInfo) {
        viewModelScope.launch {
            repository.updateVehicleInfo(info).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _vehicleInfo.value = res.data
                }
            }
        }
    }

    fun uploadDocument(type: String, url: String) {
        viewModelScope.launch {
            repository.uploadDocument(type, url).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _driverDocuments.value = _driverDocuments.value.filter { it.type != type } + res.data
                    updateApprovalStatus()
                }
            }
        }
    }

    fun fetchTrips() {
        viewModelScope.launch {
            repository.getTrips().collect { _trips.value = it }
        }
    }

    fun fetchRideOptions() {
        viewModelScope.launch {
            repository.getRideOptions().collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    _rideOptions.value = resource.data
                }
            }
        }
    }

    fun fetchDestinations() {
        viewModelScope.launch { repository.getDestinations().collect { _destinations.value = it } }
    }

    fun saveDestination(name: String, address: String, latitude: Double = 0.0, longitude: Double = 0.0, isFavorite: Boolean = false, type: String = "recent") {
        viewModelScope.launch { repository.saveDestination(name, address, latitude, longitude, isFavorite, type).collect { } }
    }

    fun deleteDestination(id: String) {
        viewModelScope.launch { repository.deleteDestination(id).collect { } }
    }

    fun togglePreference(pref: String) {
        _ridePreferences[pref] = !(_ridePreferences[pref] ?: false)
    }

    fun selectRide(option: RideOption?) {
        _selectedRide.value = option
    }



    fun searchPlaces(context: Context, query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300)
            val placesClient = Places.createClient(context)
            val token = AutocompleteSessionToken.newInstance()
            val request = FindAutocompletePredictionsRequest.builder().setSessionToken(token).setQuery(query).build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    val newSuggestions = response.autocompletePredictions.map { prediction ->
                        Destination(id = prediction.placeId, name = prediction.getPrimaryText(null).toString(), address = prediction.getSecondaryText(null).toString(), placeId = prediction.placeId)
                    }
                    _suggestions.value = newSuggestions
                    _isSearching.value = false
                }
                .addOnFailureListener { _isSearching.value = false }
        }
    }

    fun updatePickupLocation(latLng: LatLng, address: String = "Current Location") {
        _pickupLocation.value = latLng
        _pickupAddress.value = address
    }

    fun updateSelectedDestination(destination: Destination?) {
        _selectedDestination.value = destination
    }

    fun reverseGeocode(context: Context, latLng: LatLng) {
        viewModelScope.launch {
            try {
                val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val addr = addresses[0].getAddressLine(0) ?: "Unknown Location"
                            _pickupAddress.value = addr
                            _pickupLocation.value = latLng
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val addr = addresses[0].getAddressLine(0) ?: "Unknown Location"
                        _pickupAddress.value = addr
                        _pickupLocation.value = latLng
                    }
                }
            } catch (e: Exception) {
                _pickupAddress.value = "${String.format(java.util.Locale.getDefault(), "%.4f", latLng.latitude)}, ${String.format(java.util.Locale.getDefault(), "%.4f", latLng.longitude)}"
                _pickupLocation.value = latLng
            }
        }
    }

    fun fetchCurrentLocation(context: Context) {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    _pickupLocation.value = latLng
                    viewModelScope.launch {
                        try {
                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(it.latitude, it.longitude, 1) { addresses ->
                                    if (addresses.isNotEmpty()) _pickupAddress.value = addresses[0].getAddressLine(0)
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                                if (addresses != null && addresses.isNotEmpty()) _pickupAddress.value = addresses[0].getAddressLine(0)
                            }
                        } catch (e: Exception) {
                            _pickupAddress.value = "Current Location"
                        }
                    }
                }
            }
        } catch (e: SecurityException) { }
    }

    fun calculateRoute(origin: LatLng, destination: LatLng, waypoints: List<LatLng> = emptyList()) {
        viewModelScope.launch(Dispatchers.IO) {
            val geoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()
            try {
                val request = DirectionsApi.newRequest(geoApiContext)
                    .mode(TravelMode.DRIVING)
                    .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                    .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))

                if (waypoints.isNotEmpty()) {
                    request.waypoints(*waypoints.map { com.google.maps.model.LatLng(it.latitude, it.longitude) }.toTypedArray())
                }

                val result = request.await()

                if (result.routes.isNotEmpty()) {
                    val route = result.routes[0]
                    val points = route.overviewPolyline.decodePath()
                    val latLngPoints = points.map { LatLng(it.lat, it.lng) }
                    val leg = route.legs[0]
                    val distanceMeters = leg.distance.inMeters
                    
                    withContext(Dispatchers.Main) {
                        if (distanceMeters > 100000) { // 100km limit for simulation
                            _outOfServiceArea.value = true
                            _routePoints.value = emptyList()
                            _trafficSegments.value = emptyList()
                            _rideOptions.value = emptyList()
                        } else {
                            _outOfServiceArea.value = false
                            _routePoints.value = latLngPoints
                            _trafficSegments.value = generateTrafficSegments(latLngPoints)
                            _routeDistance.value = leg.distance.humanReadable
                            _routeDuration.value = leg.duration.humanReadable
                            calculateRideOptions(distanceMeters)
                        }
                    }
                } else {
                    throw Exception("No routes found")
                }
            } catch (e: Exception) {
                // Fallback: Estimate distance if Directions API fails (e.g. API key issue)
                val results = FloatArray(1)
                android.location.Location.distanceBetween(origin.latitude, origin.longitude, destination.latitude, destination.longitude, results)
                val directDistanceMeters = results[0].toLong()
                // Driving distance is usually ~30% longer than straight line
                val estimatedDrivingDistanceMeters = (directDistanceMeters * 1.3).toLong()
                val estimatedKm = estimatedDrivingDistanceMeters / 1000.0
                
                withContext(Dispatchers.Main) {
                    _outOfServiceArea.value = estimatedKm > 100.0
                    _routeDistance.value = String.format(Locale.getDefault(), "%.1f km", estimatedKm)
                    _routeDuration.value = String.format(Locale.getDefault(), "%d min", (estimatedKm * 2).toInt() + 2)
                    calculateRideOptions(estimatedDrivingDistanceMeters)
                }
            } finally { geoApiContext.shutdown() }
        }
    }

    private fun generateTrafficSegments(points: List<LatLng>): List<TrafficSegment> {
        if (points.size < 2) return emptyList()
        val segments = mutableListOf<TrafficSegment>()
        var currentIndex = 0
        while (currentIndex < points.size - 1) {
            val segmentSize = Random.nextInt(5, 15)
            val nextIndex = (currentIndex + segmentSize).coerceAtMost(points.size - 1)
            val segmentPoints = points.subList(currentIndex, nextIndex + 1)
            val color = when (Random.nextInt(3)) {
                0 -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
                1 -> androidx.compose.ui.graphics.Color(0xFFFFC107) // Yellow
                else -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
            }
            segments.add(TrafficSegment(segmentPoints, color))
            currentIndex = nextIndex
            if (currentIndex == points.size - 1) break
        }
        return segments
    }

    fun clearRoute() {
        _routePoints.value = emptyList()
        _routeDistance.value = null
        _routeDuration.value = null
        _rideOptions.value = emptyList()
        _outOfServiceArea.value = false
    }

    fun startBooking() {
        val pickup = _pickupLocation.value ?: return
        val dest = _selectedRide.value ?: return
        val pickupAddr = _pickupAddress.value
        val destAddr = _selectedDestination.value?.address ?: "Destination"
        val dLat = _selectedDestination.value?.latitude ?: 0.0
        val dLng = _selectedDestination.value?.longitude ?: 0.0
        val distance = _routeDistance.value ?: "Calculated"
        
        viewModelScope.launch {
            _bookingStatus.value = BookingStatus.SEARCHING
            _assignedDriver.value = null
            _safetyPin.value = null
            
            repository.createRideRequest(
                pickup = pickupAddr,
                dest = destAddr,
                pLat = pickup.latitude,
                pLng = pickup.longitude,
                dLat = dLat,
                dLng = dLng,
                fare = dest.price.toDoubleOrNull() ?: 0.0,
                distance = distance,
                type = dest.name
            ).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _activeTripId.value = res.data
                } else if (res is Resource.Error) {
                    _bookingStatus.value = BookingStatus.NONE
                }
            }
        }
    }

    fun clearTripSummary() {
        _completedTripSummary.value = null
        _bookingStatus.value = BookingStatus.NONE
    }

    fun cancelBooking() {
        val tripId = _activeTripId.value ?: "current"
        viewModelScope.launch {
            repository.cancelRide(tripId, "User cancelled", "PASSENGER").collect { res ->
                if (res is Resource.Success) {
                    _assignedDriver.value = null
                    _bookingStatus.value = BookingStatus.NONE
                    _activeTripId.value = null
                }
            }
        }
    }

    private fun calculateRideOptions(meters: Long) {
        val km = meters / 1000.0
        val baseFare = 20.0
        
        val currentOptions = _rideOptions.value
        if (currentOptions.isNotEmpty()) {
            _rideOptions.value = currentOptions.map { option ->
                val multiplier = when (option.name) {
                    "TJ Go" -> 7.0
                    "TJ Comfort" -> 10.0
                    "TJ Premium" -> 18.0
                    "TJ XL" -> 12.0
                    else -> 8.5
                }
                option.copy(
                    price = String.format(Locale.getDefault(), "%.2f", baseFare + (km * multiplier)),
                    arrivalTime = "${Random.nextInt(2, 8)} min"
                )
            }
        } else {
            _rideOptions.value = listOf(
                RideOption("1", "TJ Go", String.format(Locale.getDefault(), "%.2f", baseFare + (km * 7.0)), arrivalTime = "3 min", description = "Standard, compact rides", iconColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)),
                RideOption("2", "TJ Comfort", String.format(Locale.getDefault(), "%.2f", baseFare + (km * 10.0)), arrivalTime = "5 min", description = "Newer cars, extra legroom", iconColor = androidx.compose.ui.graphics.Color(0xFF2196F3)),
                RideOption("3", "TJ Premium", String.format(Locale.getDefault(), "%.2f", baseFare + (km * 18.0)), arrivalTime = "4 min", description = "Luxury & top drivers", iconColor = androidx.compose.ui.graphics.Color(0xFF9C27B0)),
                RideOption("4", "TJ XL", String.format(Locale.getDefault(), "%.2f", baseFare + (km * 12.0)), arrivalTime = "7 min", description = "SUVs for groups up to 6", iconColor = androidx.compose.ui.graphics.Color(0xFFFF9800))
            )
        }
    }

    fun getPlaceDetails(context: Context, destination: Destination, onComplete: (Destination) -> Unit) {
        val placeId = destination.placeId
        if (placeId == null) { onComplete(destination); return }
        
        val placesClient = Places.createClient(context)
        val placeFields = listOf(Place.Field.LOCATION, Place.Field.FORMATTED_ADDRESS)
        val request = FetchPlaceRequest.newInstance(placeId, placeFields)
        
        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            val latLng = place.location
            if (latLng != null) {
                onComplete(destination.copy(
                    latitude = latLng.latitude, 
                    longitude = latLng.longitude, 
                    address = place.formattedAddress ?: destination.address
                ))
            } else {
                onComplete(destination)
            }
        }.addOnFailureListener { 
            onComplete(destination) 
        }
    }
}
