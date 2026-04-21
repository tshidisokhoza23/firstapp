@file:Suppress("DEPRECATION")

package com.example.firstapp.ui.screens

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.model.Destination
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NearbyDriver(
    val id: String,
    val position: LatLng,
    val rotation: Float
)

class DriveViewModel : ViewModel() {
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

    private val _routeDistance = mutableStateOf<String?>(null)
    val routeDistance: State<String?> = _routeDistance

    private val _routeDuration = mutableStateOf<String?>(null)
    val routeDuration: State<String?> = _routeDuration

    private val _rideOptions = mutableStateOf<List<com.example.firstapp.model.RideOption>>(emptyList())
    val rideOptions: State<List<com.example.firstapp.model.RideOption>> = _rideOptions

    private val _nearbyDrivers = mutableStateOf<List<NearbyDriver>>(emptyList())
    val nearbyDrivers: State<List<NearbyDriver>> = _nearbyDrivers

    private val _assignedDriver = mutableStateOf<NearbyDriver?>(null)
    val assignedDriver: State<NearbyDriver?> = _assignedDriver

    private var searchJob: Job? = null
    private var driversJob: Job? = null
    private var bookingJob: Job? = null
    private val apiKey = "AIzaSyAcwjsoRCNMFsX5ayuuYC1CoNqj8NVP3Kw"

    init {
        startSimulatingDrivers()
    }

    private fun startSimulatingDrivers() {
        driversJob?.cancel()
        driversJob = viewModelScope.launch {
            while(true) {
                val center = _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
                if (_nearbyDrivers.value.isEmpty()) {
                    // Initialize drivers
                    _nearbyDrivers.value = List(6) { i ->
                        NearbyDriver(
                            id = "driver_$i",
                            position = LatLng(
                                center.latitude + (Random.nextDouble() - 0.5) * 0.015,
                                center.longitude + (Random.nextDouble() - 0.5) * 0.015
                            ),
                            rotation = Random.nextFloat() * 360f
                        )
                    }
                } else {
                    // Move drivers slightly
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
            delay(300) // Debounce
            
            val placesClient = Places.createClient(context)
            val token = AutocompleteSessionToken.newInstance()
            
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(token)
                .setQuery(query)
                .build()

            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    val newSuggestions = response.autocompletePredictions.map { prediction ->
                        Destination(
                            id = prediction.placeId.hashCode(),
                            name = prediction.getPrimaryText(null).toString(),
                            address = prediction.getSecondaryText(null).toString(),
                            placeId = prediction.placeId
                        )
                    }
                    _suggestions.value = newSuggestions
                    _isSearching.value = false
                }
                .addOnFailureListener {
                    _isSearching.value = false
                }
        }
    }

    fun updatePickupLocation(latLng: LatLng, address: String = "Current Location") {
        _pickupLocation.value = latLng
        _pickupAddress.value = address
    }

    fun fetchCurrentLocation(context: Context) {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng(it.latitude, it.longitude)
                    _pickupLocation.value = latLng
                    
                    // Reverse geocode to get readable address
                    viewModelScope.launch {
                        try {
                            val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                geocoder.getFromLocation(it.latitude, it.longitude, 1) { addresses ->
                                    if (addresses.isNotEmpty()) {
                                        val address = addresses[0].getAddressLine(0)
                                        _pickupAddress.value = address
                                    }
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                                if (addresses != null && addresses.isNotEmpty()) {
                                    val address = addresses[0].getAddressLine(0)
                                    _pickupAddress.value = address
                                }
                            }
                        } catch (e: Exception) {
                            _pickupAddress.value = "Current Location"
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun calculateRoute(origin: LatLng, destination: LatLng) {
        viewModelScope.launch(Dispatchers.IO) {
            val geoApiContext = GeoApiContext.Builder()
                .apiKey(apiKey)
                .build()

            try {
                val result = DirectionsApi.newRequest(geoApiContext)
                    .mode(TravelMode.DRIVING)
                    .origin(com.google.maps.model.LatLng(origin.latitude, origin.longitude))
                    .destination(com.google.maps.model.LatLng(destination.latitude, destination.longitude))
                    .await()

                if (result.routes.isNotEmpty()) {
                    val route = result.routes[0]
                    val points = route.overviewPolyline.decodePath()
                    val latLngPoints = points.map { LatLng(it.lat, it.lng) }
                    
                    val leg = route.legs[0]
                    val distanceText = leg.distance.humanReadable
                    val durationText = leg.duration.humanReadable
                    val distanceValueMeters = leg.distance.inMeters

                    withContext(Dispatchers.Main) {
                        _routePoints.value = latLngPoints
                        _routeDistance.value = distanceText
                        _routeDuration.value = durationText
                        calculateRideOptions(distanceValueMeters)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                geoApiContext.shutdown()
            }
        }
    }

    fun clearRoute() {
        _routePoints.value = emptyList()
        _routeDistance.value = null
        _routeDuration.value = null
        _rideOptions.value = emptyList()
    }

    fun startBooking() {
        bookingJob?.cancel()
        bookingJob = viewModelScope.launch {
            // Simulate "Finding your driver" phase
            _assignedDriver.value = null
            delay(3000)
            
            val pickup = _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
            
            // Pick a nearby driver or create one
            val driver = _nearbyDrivers.value.firstOrNull()?.copy(id = "assigned_driver") 
                ?: NearbyDriver("assigned_driver", LatLng(pickup.latitude + 0.005, pickup.longitude + 0.005), 0f)
            
            _assignedDriver.value = driver
            // Filter out from nearby
            _nearbyDrivers.value = _nearbyDrivers.value.filter { it.id != driver.id }
            
            // Simulate movement towards pickup point
            val startPos = driver.position
            val steps = 40
            for (i in 1..steps) {
                delay(2000)
                val progress = i.toFloat() / steps
                val currentLat = startPos.latitude + (pickup.latitude - startPos.latitude) * progress
                val currentLng = startPos.longitude + (pickup.longitude - startPos.longitude) * progress
                
                // Calculate bearing for rotation
                val rotation = Math.toDegrees(
                    Math.atan2(pickup.longitude - startPos.longitude, pickup.latitude - startPos.latitude)
                ).toFloat()
                
                _assignedDriver.value = _assignedDriver.value?.copy(
                    position = LatLng(currentLat, currentLng),
                    rotation = rotation
                )
                
                if (progress >= 1.0f) break
            }
        }
    }

    fun cancelBooking() {
        bookingJob?.cancel()
        _assignedDriver.value = null
    }

    private fun calculateRideOptions(meters: Long) {
        val km = meters / 1000.0
        val baseFare = 20.0
        val perKmGo = 7.0
        val perKmComfort = 10.0
        val perKmPremium = 18.0
        val perKmXL = 12.0

        _rideOptions.value = listOf(
            com.example.firstapp.model.RideOption(
                1, "FirstApp Go", 
                String.format(Locale.getDefault(), "%.2f", baseFare + (km * perKmGo)), 
                arrivalTime = "3 min", 
                description = "Standard, compact rides", 
                iconColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
            ),
            com.example.firstapp.model.RideOption(
                2, "FirstApp Comfort", 
                String.format(Locale.getDefault(), "%.2f", baseFare + (km * perKmComfort)), 
                arrivalTime = "5 min", 
                description = "Newer cars, extra legroom", 
                iconColor = androidx.compose.ui.graphics.Color(0xFF2196F3)
            ),
            com.example.firstapp.model.RideOption(
                3, "FirstApp Premium", 
                String.format(Locale.getDefault(), "%.2f", baseFare + (km * perKmPremium)), 
                arrivalTime = "4 min", 
                description = "Luxury & top drivers", 
                iconColor = androidx.compose.ui.graphics.Color(0xFF9C27B0)
            ),
            com.example.firstapp.model.RideOption(
                4, "FirstApp XL", 
                String.format(Locale.getDefault(), "%.2f", baseFare + (km * perKmXL)),
                arrivalTime = "7 min", 
                description = "SUVs for groups up to 6", 
                iconColor = androidx.compose.ui.graphics.Color(0xFFFF9800)
            )
        )
    }

    fun getPlaceDetails(context: Context, destination: Destination, onComplete: (Destination) -> Unit) {
        if (destination.placeId == null) {
            onComplete(destination)
            return
        }

        val placesClient = Places.createClient(context)
        val placeFields = listOf(
            com.google.android.libraries.places.api.model.Place.Field.LAT_LNG,
            com.google.android.libraries.places.api.model.Place.Field.ADDRESS
        )
        val request = FetchPlaceRequest.newInstance(destination.placeId, placeFields)

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                val latLng = place.latLng
                if (latLng != null) {
                    onComplete(
                        destination.copy(
                            latitude = latLng.latitude,
                            longitude = latLng.longitude,
                            address = place.address ?: destination.address
                        )
                    )
                } else {
                    onComplete(destination)
                }
            }
            .addOnFailureListener {
                onComplete(destination)
            }
    }
}
