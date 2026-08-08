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

    private val _incomingRideRequest = MutableStateFlow<RideRequest?>(null)
    val incomingRideRequest: StateFlow<RideRequest?> = _incomingRideRequest.asStateFlow()

    private val _driverEarnings = MutableStateFlow(0.0)
    val driverEarnings: StateFlow<Double> = _driverEarnings.asStateFlow()

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
        _driverDocuments.value = listOf(
            DriverDocument("1", "Driver's License", "APPROVED"),
            DriverDocument("2", "Vehicle Registration", "PENDING"),
            DriverDocument("3", "Identity Document", "APPROVED")
        )
    }

    fun toggleDriverMode(enabled: Boolean) {
        _isDriverMode.value = enabled
        if (enabled) {
            startDriverSimulation()
        } else {
            simulationJob?.cancel()
            _incomingRideRequest.value = null
        }
    }

    fun setDriverOnlineStatus(online: Boolean) {
        _driverOnlineStatus.value = online
        if (online) {
            startDriverSimulation()
        } else {
            _incomingRideRequest.value = null
        }
    }

    private fun startDriverSimulation() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            while(true) {
                if (_driverOnlineStatus.value && _incomingRideRequest.value == null) {
                    delay(8000) // Random wait for a request
                    _incomingRideRequest.value = RideRequest(
                        id = "req_${System.currentTimeMillis()}",
                        passengerName = "Sarah Smith",
                        pickupAddress = "Sandton City Mall",
                        destinationAddress = "OR Tambo International",
                        fare = 350.00,
                        distance = "24 km"
                    )
                }
                delay(5000)
            }
        }
    }

    fun acceptRideRequest() {
        viewModelScope.launch {
            val req = _incomingRideRequest.value ?: return@launch
            _incomingRideRequest.value = null
            // In a real app, this would start the navigation/trip
            _bookingStatus.value = BookingStatus.EN_ROUTE
            _driverEarnings.value += req.fare
        }
    }

    fun declineRideRequest() {
        _incomingRideRequest.value = null
    }

    fun uploadDocument(type: String, url: String) {
        val newDoc = DriverDocument(
            id = System.currentTimeMillis().toString(),
            type = type,
            status = "PENDING",
            url = url
        )
        _driverDocuments.value = _driverDocuments.value + newDoc
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
            val names = listOf("Thabo", "Lindiwe", "Sipho", "Sarah", "Johannes", "Tumi")
            val cars = listOf("Toyota Corolla", "VW Polo", "Honda Civic", "Hyundai i20", "Ford Fiesta", "Mazda 3")
            val colors = listOf("White", "Silver", "Black", "Grey", "Blue", "Red")
            
            while(true) {
                val center = _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
                if (_nearbyDrivers.value.isEmpty()) {
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
                delay(1000)
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
        bookingJob?.cancel()
        bookingJob = viewModelScope.launch {
            _bookingStatus.value = BookingStatus.SEARCHING
            _assignedDriver.value = null
            _safetyPin.value = (1000..9999).random().toString()
            delay(3000)
            
            val pickup = _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
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
            
            // Simulated wallet deduction
            val priceStr = _selectedRide.value?.price ?: "0.00"
            val price = priceStr.toDoubleOrNull() ?: 0.0
            _walletBalance.value = (_walletBalance.value - price).coerceAtLeast(0.0)
        }
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
