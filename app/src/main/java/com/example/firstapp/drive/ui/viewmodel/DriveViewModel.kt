@file:Suppress("DEPRECATION")

package com.example.firstapp.drive.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.drive.domain.repository.DriveRepository
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.model.*
import com.example.firstapp.drive.model.*
import com.example.firstapp.common.util.Resource
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

data class NearbyDriver(
    val id: String,
    val position: LatLng,
    val rotation: Float,
    val name: String = "Mpho",
    val rating: Double = 4.8,
    val vehicleModel: String = "BMW 3 Series",
    val vehicleColor: String = "White",
    val licensePlate: String = "GP 123 456"
)

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

    data class DriverStats(
        val totalTrips: Int = 42,
        val onlineHours: Double = 5.5,
        val acceptanceRate: Double = 98.0,
        val weeklyEarnings: Double = 2450.0
    )

    data class TripSummary(
        val fare: Double,
        val distance: String,
        val duration: String,
        val date: String = "Today, 14:20",
        val passengerName: String,
        val paymentMethod: String = "Wallet"
    )

    data class VehicleInfo(
        val make: String = "",
        val model: String = "",
        val year: String = "",
        val color: String = "",
        val license_plate: String = ""
    )

    data class DriverStatus(
        val user_id: String,
        val is_online: Boolean,
        val last_lat: Double,
        val last_lng: Double,
        val updated_at: Long
    )

    private var searchJob: Job? = null
    private var driversJob: Job? = null
    private var bookingJob: Job? = null
    private var simulationJob: Job? = null
    private val apiKey = "AIzaSyAcwjsoRCNMFsX5ayuuYC1CoNqj8NVP3Kw"

    enum class BookingStatus {
        NONE, SEARCHING, EN_ROUTE, ARRIVED, IN_PROGRESS, COMPLETED
    }

    data class DriverDocument(
        val id: String,
        val type: String,
        val status: String, // "PENDING", "APPROVED", "REJECTED"
        val url: String? = null
    )

    data class RideRequest(
        val id: String,
        val passengerName: String,
        val pickupAddress: String,
        val destinationAddress: String,
        val fare: Double,
        val distance: String,
        val passengerRating: Double = 4.9
    )

    init {
        fetchDestinations()
        fetchTrips()
        startSimulatingDrivers()
        loadDriverState()
    }

    private fun loadDriverState() {
        viewModelScope.launch {
            repository.getDriverDocuments().collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _driverDocuments.value = res.data
                    updateApprovalStatus()
                } else if (res is Resource.Error) {
                    // Fallback to mock if server fails for now
                    _driverDocuments.value = listOf(
                        DriverDocument("1", "ID Document", "APPROVED"),
                        DriverDocument("2", "Driver's License", "APPROVED"),
                        DriverDocument("3", "Criminal Verification", "PENDING"),
                        DriverDocument("4", "Roadworthy Certificate", "PENDING"),
                        DriverDocument("5", "Car Image", "APPROVED")
                    )
                    updateApprovalStatus()
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

    fun toggleDriverMode(enabled: Boolean) {
        _isDriverMode.value = enabled
        if (enabled && _isDriverApproved.value) {
            startDriverSimulation()
        } else {
            simulationJob?.cancel()
            _incomingRideRequest.value = null
            setDriverOnlineStatus(false)
        }
    }

    fun setDriverOnlineStatus(online: Boolean) {
        _driverOnlineStatus.value = online
        val pos = _driverPosition.value ?: _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
        viewModelScope.launch {
            repository.updateOnlineStatus(online, pos.latitude, pos.longitude).collect { }
        }
        if (online && _isDriverApproved.value) {
            startDriverSimulation()
        } else {
            simulationJob?.cancel()
            _incomingRideRequest.value = null
        }
    }

    private fun startDriverSimulation() {
        if (!_isDriverApproved.value) return
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            _driverPosition.value = _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
            while(true) {
                if (_driverOnlineStatus.value) {
                    // 1. Simulate/Update location
                    val current = _driverPosition.value
                    val lastKnownPos = if (current != null) {
                        val newPos = LatLng(
                            current.latitude + (Random.nextDouble() - 0.5) * 0.0005,
                            current.longitude + (Random.nextDouble() - 0.5) * 0.0005
                        )
                        _driverPosition.value = newPos
                        // Update status/location on server
                        repository.updateOnlineStatus(true, newPos.latitude, newPos.longitude).collect { }
                        newPos
                    } else {
                        _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
                    }

                    // 2. Refresh Driver Stats periodically
                    if (Random.nextInt(10) == 0) { // roughly every 50s
                         _driverStats.value = _driverStats.value.copy(
                             onlineHours = _driverStats.value.onlineHours + 0.01,
                             weeklyEarnings = _driverEarnings.value + 2450.0 // simulated base
                         )
                    }

                    // 3. Fetch requests from server or simulate
                    if (_incomingRideRequest.value == null && _bookingStatus.value == BookingStatus.NONE) {
                        repository.getAvailableRequests(lastKnownPos.latitude, lastKnownPos.longitude).collect { res ->
                            if (res is Resource.Success && res.data != null && res.data.isNotEmpty()) {
                                _incomingRideRequest.value = res.data.first()
                            } else {
                                // Simulation fallback with higher probability for testing
                                if (Random.nextInt(100) < 20) {
                                    _incomingRideRequest.value = RideRequest(
                                        id = "req_${System.currentTimeMillis()}",
                                        passengerName = "Sarah Smith",
                                        pickupAddress = "Sandton City Mall",
                                        destinationAddress = "OR Tambo International",
                                        fare = 350.00,
                                        distance = "24 km"
                                    )
                                }
                            }
                        }
                    }
                }
                delay(5000)
            }
        }
    }

    fun acceptRideRequest() {
        viewModelScope.launch {
            val req = _incomingRideRequest.value ?: return@launch
            
            repository.updateRideStatus(req.id, "ACCEPTED").collect { res ->
                if (res is Resource.Success) {
                    _incomingRideRequest.value = null
                    _bookingStatus.value = BookingStatus.EN_ROUTE
                    _driverEarnings.value += req.fare
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

    private fun startSimulatingDrivers() {
        driversJob?.cancel()
        driversJob = viewModelScope.launch {
            while(true) {
                val center = _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
                
                // Fetch real nearby drivers from server
                repository.getNearbyDrivers(center.latitude, center.longitude).collect { res ->
                    if (res is Resource.Success && res.data != null && res.data.isNotEmpty()) {
                        _nearbyDrivers.value = res.data.map { status ->
                            NearbyDriver(
                                id = status.user_id,
                                position = LatLng(status.last_lat, status.last_lng),
                                rotation = 0f,
                                name = "Driver", // Ideally fetch name
                                vehicleModel = "Active Vehicle"
                            )
                        }
                    } else if (res is Resource.Error || (res is Resource.Success && res.data.isNullOrEmpty())) {
                        // Fallback to simulation if no real drivers online
                        val names = listOf("Thabo", "Lindiwe", "Sipho", "Sarah", "Johannes", "Tumi")
                        val cars = listOf("Toyota Corolla", "VW Polo", "Honda Civic", "Hyundai i20", "Ford Fiesta", "Mazda 3")
                        val colors = listOf("White", "Silver", "Black", "Grey", "Blue", "Red")
                        
                        if (_nearbyDrivers.value.isEmpty() || _nearbyDrivers.value.all { !it.id.startsWith("driver_") }) {
                            _nearbyDrivers.value = List(6) { i ->
                                NearbyDriver(
                                    id = "driver_$i",
                                    position = LatLng(
                                        center.latitude + (Random.nextDouble() - 0.5) * 0.015,
                                        center.longitude + (Random.nextDouble() - 0.5) * 0.015
                                    ),
                                    rotation = Random.nextFloat() * 360f,
                                    name = names[i % names.size],
                                    vehicleModel = cars[i % cars.size],
                                    vehicleColor = colors[i % colors.size],
                                    licensePlate = "GP ${Random.nextInt(100, 999)} ${Random.nextInt(100, 999)}"
                                )
                            }
                        } else {
                            _nearbyDrivers.value = _nearbyDrivers.value.map { driver ->
                                val latMovement = (Random.nextDouble() - 0.5) * 0.0001
                                val lngMovement = (Random.nextDouble() - 0.5) * 0.0001
                                driver.copy(
                                    position = LatLng(
                                        driver.position.latitude + latMovement,
                                        driver.position.longitude + lngMovement
                                    ),
                                    rotation = (driver.rotation + (Random.nextFloat() - 0.5f) * 10f) % 360f
                                )
                            }
                        }
                    }
                }
                delay(10000) // Poll every 10s for nearby drivers
            }
        }
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
                }
            } catch (e: Exception) { } finally { geoApiContext.shutdown() }
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
        val destAddr = _suggestions.value.firstOrNull()?.address ?: "Destination"
        
        bookingJob?.cancel()
        bookingJob = viewModelScope.launch {
            _bookingStatus.value = BookingStatus.SEARCHING
            _assignedDriver.value = null
            _safetyPin.value = (1000..9999).random().toString()
            
            // 1. Create request on server
            repository.createRideRequest(
                pickup = pickupAddr,
                dest = destAddr,
                pLat = pickup.latitude,
                pLng = pickup.longitude,
                dLat = 0.0,
                dLng = 0.0,
                fare = dest.price.toDoubleOrNull() ?: 0.0,
                type = dest.name
            ).collect { }

            delay(4000)
            
            val driver = _nearbyDrivers.value.firstOrNull()?.copy(id = "assigned_driver") 
                ?: NearbyDriver(
                    id = "assigned_driver", 
                    position = LatLng(pickup.latitude + 0.005, pickup.longitude + 0.005), 
                    rotation = 0f,
                    name = "Mpho",
                    vehicleModel = "BMW 3 Series",
                    licensePlate = "GP 123 456"
                )
            
            _assignedDriver.value = driver
            _nearbyDrivers.value = _nearbyDrivers.value.filter { it.id != driver.id }
            _bookingStatus.value = BookingStatus.EN_ROUTE
            
            // Move to pickup
            val startPos = driver.position
            val pickupSteps = 20
            for (i in 1..pickupSteps) {
                delay(1000)
                val progress = i.toFloat() / pickupSteps
                _remainingTime.value = "${((pickupSteps - i) * 1).coerceAtLeast(1)} min"
                val currentLat = startPos.latitude + (pickup.latitude - startPos.latitude) * progress
                val currentLng = startPos.longitude + (pickup.longitude - startPos.longitude) * progress
                val rotation = Math.toDegrees(Math.atan2(pickup.longitude - startPos.longitude, pickup.latitude - startPos.latitude)).toFloat()
                _assignedDriver.value = _assignedDriver.value?.copy(position = LatLng(currentLat, currentLng), rotation = rotation)
            }
            
            _bookingStatus.value = BookingStatus.ARRIVED
            _remainingTime.value = "Arrived"
            delay(5000) // Wait for user to get in
            
            _bookingStatus.value = BookingStatus.IN_PROGRESS
            // Simulation of trip to destination if we have route points
            val points = _routePoints.value
            if (points.isNotEmpty()) {
                val totalPoints = points.size
                points.forEachIndexed { index, point ->
                    if (index % 5 == 0) { // Move in chunks to avoid too many updates
                        delay(1000)
                        _remainingTime.value = "${((totalPoints - index) / 10).coerceAtLeast(1)} min"
                        val prevPoint = if (index > 0) points[index - 1] else pickup
                        val rotation = Math.toDegrees(Math.atan2(point.longitude - prevPoint.longitude, point.latitude - prevPoint.latitude)).toFloat()
                        _assignedDriver.value = _assignedDriver.value?.copy(position = point, rotation = rotation)
                    }
                }
            }
            
            _bookingStatus.value = BookingStatus.COMPLETED
            _remainingTime.value = "Finished"
            
            val fare = dest.price.toDoubleOrNull() ?: 0.0
            _completedTripSummary.value = TripSummary(
                fare = fare,
                distance = _routeDistance.value ?: "0 km",
                duration = _routeDuration.value ?: "0 min",
                passengerName = driver.name
            )

            // Simulated wallet deduction
            _walletBalance.value = (_walletBalance.value - fare).coerceAtLeast(0.0)
        }
    }

    fun clearTripSummary() {
        _completedTripSummary.value = null
        _bookingStatus.value = BookingStatus.NONE
    }

    fun cancelBooking() {
        bookingJob?.cancel()
        _assignedDriver.value = null
        _bookingStatus.value = BookingStatus.NONE
    }

    private fun calculateRideOptions(meters: Long) {
        val km = meters / 1000.0
        val baseFare = 20.0
        _rideOptions.value = listOf(
            RideOption("1", "TJ Go", String.format(Locale.getDefault(), "%.2f", baseFare + (km * 7.0)), arrivalTime = "3 min", description = "Standard, compact rides", iconColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)),
            RideOption("2", "TJ Comfort", String.format(Locale.getDefault(), "%.2f", baseFare + (km * 10.0)), arrivalTime = "5 min", description = "Newer cars, extra legroom", iconColor = androidx.compose.ui.graphics.Color(0xFF2196F3)),
            RideOption("3", "TJ Premium", String.format(Locale.getDefault(), "%.2f", baseFare + (km * 18.0)), arrivalTime = "4 min", description = "Luxury & top drivers", iconColor = androidx.compose.ui.graphics.Color(0xFF9C27B0)),
            RideOption("4", "TJ XL", String.format(Locale.getDefault(), "%.2f", baseFare + (km * 12.0)), arrivalTime = "7 min", description = "SUVs for groups up to 6", iconColor = androidx.compose.ui.graphics.Color(0xFFFF9800))
        )
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
