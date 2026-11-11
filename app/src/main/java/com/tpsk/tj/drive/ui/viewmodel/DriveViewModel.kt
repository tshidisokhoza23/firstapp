package com.tpsk.tj.drive.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.tpsk.tj.drive.domain.repository.DriveRepository
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
import com.tpsk.tj.common.model.*
import com.tpsk.tj.drive.model.*
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.drive.data.network.RideEvent
import com.tpsk.tj.drive.data.service.DriverLocationService
import com.tpsk.tj.drive.ui.RideRequestAlerts
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.maps.DirectionsApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.maps.GeoApiContext
import com.google.maps.model.TrafficModel
import com.google.maps.model.TravelMode
import java.util.Locale
import kotlin.math.floor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



@HiltViewModel
class DriveViewModel @Inject constructor(
    private val repository: DriveRepository,
    private val chatRepository: com.tpsk.tj.domain.repository.ChatRepository
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
    private var routeMeters = 0L
    private var routeSeconds = 0L
    private val _tripStops = mutableStateListOf<Destination>()
    val tripStops: List<Destination> = _tripStops
    private val _areaPresence = mutableStateOf<AreaPresence?>(null)
    val areaPresence: State<AreaPresence?> = _areaPresence
    private val _areaError = mutableStateOf<String?>(null)
    val areaError: State<String?> = _areaError
    private val _activeStops = mutableStateOf<List<ActiveStop>>(emptyList())
    val activeStops: State<List<ActiveStop>> = _activeStops
    private val _arrivedAt = mutableStateOf(0L)
    val arrivedAt: State<Long> = _arrivedAt
    private var livePickup: LatLng? = null
    private var liveDest: LatLng? = null
    private var lastEtaAt = 0L
    private var lastAreaAt = 0L
    private var areaJob: Job? = null

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

    private val _walletBalance = mutableStateOf(0.0)
    val walletBalance: State<Double> = _walletBalance

    private val _feePlan = mutableStateOf(DriverFeePlan())
    val feePlan: State<DriverFeePlan> = _feePlan

    private val _actionError = mutableStateOf<String?>(null)
    val actionError: State<String?> = _actionError

    private val _liveFare = mutableStateOf(0.0)
    private var protectedWeeklyTrip = false
    private var pendingWeekEndedNotice = false
    private var pollJob: Job? = null

    private val _transactions = mutableStateListOf<Transaction>()
    val transactions: List<Transaction> = _transactions
    private val _availablePayout = mutableStateOf(0.0)
    val availablePayout: State<Double> = _availablePayout

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
            locationService?.onMoved = { lat, lng -> noteDriverPosition(lat, lng) }
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            locationService?.onMoved = null
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
    private val mapboxAccessToken = "pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpej" + "Y4NXVycTA2emYycXBndHRqcm53N3MifQ.rJcFIG214AriISLbB6B5aw"

    private var searchJob: Job? = null

    init {
        fetchDestinations()
        fetchTrips()
        fetchRideOptions()
        refreshPayments()
        loadDriverState()
        loadVehicle()
        loadFeePlan()
        observeRideEvents()
        repository.connectRideSocket()
        refreshRideState()
        startPolling()
    }

    fun lockRole(driver: Boolean) {
        if (_bookingStatus.value != BookingStatus.NONE && _bookingStatus.value != BookingStatus.COMPLETED && _isDriverMode.value != driver) {
            return
        }
        _isDriverMode.value = driver
        if (driver) {
            loadVehicle()
            loadFeePlan()
        }
    }

    fun consumeActionError() {
        _actionError.value = null
    }

    fun chooseFeePlan(plan: String) {
        val current = _feePlan.value
        if (current.on_trip || (_bookingStatus.value != BookingStatus.NONE && _bookingStatus.value != BookingStatus.COMPLETED)) {
            _actionError.value = "Finish this ride first. Your pay option stays the same until the trip is done."
            return
        }
        if (plan != "COMMISSION") {
            _actionError.value = "Only 20% per trip is available right now."
            return
        }
        viewModelScope.launch {
            repository.setDriverPlan(plan).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _feePlan.value = res.data
                    _walletBalance.value = res.data.wallet_balance
                } else if (res is Resource.Error) {
                    _actionError.value = res.message
                }
            }
        }
    }

    private fun loadFeePlan() {
        viewModelScope.launch {
            repository.getDriverPlan().collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _feePlan.value = res.data
                    _walletBalance.value = res.data.wallet_balance
                    _driverEarnings.value = res.data.week_earnings
                    if (pendingWeekEndedNotice && !res.data.on_trip) {
                        pendingWeekEndedNotice = false
                        _actionError.value = "That ride kept its original rate. New rides use 20%."
                    }
                }
            }
        }
    }

    private fun loadVehicle() {
        viewModelScope.launch {
            repository.getVehicleInfo().collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _vehicleInfo.value = res.data
                    updateApprovalStatus()
                }
            }
        }
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                val busy = _bookingStatus.value != BookingStatus.NONE && _bookingStatus.value != BookingStatus.COMPLETED
                if (busy || _activeTripId.value != null) {
                    refreshRideState()
                }
                if (_isDriverMode.value) {
                    loadFeePlan()
                }
                if (_isDriverMode.value && _driverOnlineStatus.value && !busy) {
                    pollRequests()
                }
                val hurry = busy || (_isDriverMode.value && _driverOnlineStatus.value)
                delay(if (hurry) 2000 else 8000)
            }
        }
    }

    private suspend fun pollRequests() {
        val pos = _driverPosition.value ?: _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
        repository.getAvailableRequests(pos.latitude, pos.longitude).collect { res ->
            if (res is Resource.Success) {
                val next = res.data?.firstOrNull { !RideRequestAlerts.isDeclined(it.id) }
                if (next != null && _incomingRideRequest.value?.id != next.id) {
                    _incomingRideRequest.value = next
                    RideRequestAlerts.alert(com.tpsk.tj.common.util.ContextProvider.context, next)
                } else if (next == null && _incomingRideRequest.value != null) {
                    RideRequestAlerts.stopIf(com.tpsk.tj.common.util.ContextProvider.context, _incomingRideRequest.value?.id.orEmpty())
                    _incomingRideRequest.value = null
                }
            }
        }
    }

    private fun refreshRideState() {
        viewModelScope.launch {
            repository.getRideState().collect { res ->
                if (res is Resource.Success && res.data != null) applyLive(res.data)
            }
        }
    }

    private fun applyLive(live: RideLiveState) {
        _walletBalance.value = live.wallet_balance
        if (live.ride_id.isNullOrBlank()) {
            val wasActive = _activeTripId.value != null && (
                _bookingStatus.value == BookingStatus.SEARCHING ||
                _bookingStatus.value == BookingStatus.EN_ROUTE ||
                _bookingStatus.value == BookingStatus.ARRIVED ||
                _bookingStatus.value == BookingStatus.IN_PROGRESS)
            if (wasActive) {
                _actionError.value = "This ride is no longer active. Any unused fare was returned to your wallet."
                _bookingStatus.value = BookingStatus.NONE
                _assignedDriver.value = null
                _activeTripId.value = null
                _safetyPin.value = null
            }
            return
        }
        _activeTripId.value = live.ride_id
        _liveFare.value = live.fare + live.wait_fee
        _arrivedAt.value = live.arrived_at
        _activeStops.value = live.stops.orEmpty()
        if (live.pickup_lat != 0.0 || live.pickup_lng != 0.0) livePickup = LatLng(live.pickup_lat, live.pickup_lng)
        if (live.dest_lat != 0.0 || live.dest_lng != 0.0) liveDest = LatLng(live.dest_lat, live.dest_lng)
        if (_isDriverMode.value && live.fee_plan == "WEEKLY" && live.status in setOf("ACCEPTED", "ARRIVED", "STARTED")) {
            protectedWeeklyTrip = true
        }
        if (!live.safety_pin.isNullOrBlank()) _safetyPin.value = live.safety_pin
        _bookingStatus.value = when (live.status) {
            "PENDING" -> BookingStatus.SEARCHING
            "ACCEPTED" -> BookingStatus.EN_ROUTE
            "ARRIVED" -> BookingStatus.ARRIVED
            "STARTED" -> BookingStatus.IN_PROGRESS
            "COMPLETED" -> BookingStatus.COMPLETED
            else -> _bookingStatus.value
        }
        if (!live.driver_name.isNullOrBlank()) {
            val current = _assignedDriver.value
            _assignedDriver.value = NearbyDriver(
                id = live.driver_id ?: current?.id ?: "assigned",
                position = if (live.driver_lat != 0.0 || live.driver_lng != 0.0) LatLng(live.driver_lat, live.driver_lng) else current?.position ?: LatLng(0.0, 0.0),
                rotation = current?.rotation ?: 0f,
                name = live.driver_name,
                vehicleModel = live.vehicle ?: current?.vehicleModel ?: "",
                licensePlate = live.plate ?: current?.licensePlate ?: "",
                photoUrls = live.vehicle_photos ?: current?.photoUrls ?: emptyList(),
                profilePhoto = live.driver_photo?.takeIf { it.isNotBlank() } ?: current?.profilePhoto.orEmpty()
            )
        } else if (live.driver_lat != 0.0 || live.driver_lng != 0.0) {
            _assignedDriver.value = _assignedDriver.value?.copy(position = LatLng(live.driver_lat, live.driver_lng))
        }
        if (!_isDriverMode.value && (live.driver_lat != 0.0 || live.driver_lng != 0.0)) {
            refreshMovingEta(LatLng(live.driver_lat, live.driver_lng))
        }
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

                            val request = RideRequest(
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
                            if (!RideRequestAlerts.isDeclined(request.id)) {
                                _incomingRideRequest.value = request
                                RideRequestAlerts.alert(com.tpsk.tj.common.util.ContextProvider.context, request)
                            }
                        }
                    }
                    is RideEvent.RequestClosed -> {
                        if (_incomingRideRequest.value?.id == event.tripId) {
                            _incomingRideRequest.value = null
                        }
                        RideRequestAlerts.stopIf(com.tpsk.tj.common.util.ContextProvider.context, event.tripId)
                        if (_activeTripId.value == event.tripId) {
                            _actionError.value = event.message
                            refreshRideState()
                        }
                    }
                    is RideEvent.RequestAccepted -> {
                        _activeTripId.value = event.tripId
                        _bookingStatus.value = BookingStatus.EN_ROUTE
                        refreshRideState()
                        _assignedDriver.value = NearbyDriver(
                            id = "assigned",
                            position = LatLng(0.0, 0.0), // Will be updated by location event
                            rotation = 0f,
                            name = event.driverName,
                            vehicleModel = event.vehicle,
                            licensePlate = event.plate,
                            photoUrls = event.photos,
                            profilePhoto = event.driverPhoto
                        )
                        
                        // Once passenger gets accepted, compute route to destination
                        val origin = _pickupLocation.value
                        val destination = _selectedDestination.value
                        if (origin != null && destination != null) {
                            calculateRoute(origin, LatLng(destination.latitude, destination.longitude), emptyList())
                        }
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
                        refreshMovingEta(LatLng(event.lat, event.lng))
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
        val approvedDocs = _driverDocuments.value.filter { it.status == "APPROVED" }.map { it.type }
        _isDriverApproved.value = RequiredDriverDocuments.all { it in approvedDocs } && _vehicleInfo.value.license_plate.isNotBlank()
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
            repository.connectRideSocket()
            if (!enabled) {
                stopLocationService()
            }
            
            _isSwitchingMode.value = false
        }
    }

    fun setDriverOnlineStatus(online: Boolean) {
        val pos = _driverPosition.value ?: _pickupLocation.value ?: LatLng(-26.1076, 28.0567)
        viewModelScope.launch {
            repository.updateOnlineStatus(online, pos.latitude, pos.longitude).collect { res ->
                if (res is Resource.Error) {
                    _actionError.value = res.message
                    if (online) _driverOnlineStatus.value = false
                    return@collect
                }
                if (res is Resource.Success) {
                    _driverOnlineStatus.value = online
                    if (online) {
                        repository.connectRideSocket()
                        startLocationService()
                        pollRequests()
                    } else {
                        stopLocationService()
                        RideRequestAlerts.stop(com.tpsk.tj.common.util.ContextProvider.context)
                        _incomingRideRequest.value = null
                    }
                }
            }
        }
    }

    private fun startLocationService() {
        val context = com.tpsk.tj.common.util.ContextProvider.context
        val intent = Intent(context, DriverLocationService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun stopLocationService() {
        val context = com.tpsk.tj.common.util.ContextProvider.context
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
            RideRequestAlerts.stop(com.tpsk.tj.common.util.ContextProvider.context)
            repository.updateRideStatus(req.id, "ACCEPTED").collect { res ->
                if (res is Resource.Success) {
                    val plan = _feePlan.value
                    if (plan.plan == "WEEKLY" || plan.active_plan == "WEEKLY") {
                        protectedWeeklyTrip = true
                    }
                    _activeTripId.value = req.id
                    _liveFare.value = req.fare
                    _incomingRideRequest.value = null
                    _bookingStatus.value = BookingStatus.EN_ROUTE
                    _pickupAddress.value = req.pickupAddress
                    _selectedDestination.value = Destination(id = req.id, name = req.destinationAddress, address = req.destinationAddress, latitude = req.destLat, longitude = req.destLng)
                    
                    // Trigger calculating the route to the passenger's pickup location
                    val driverPos = _driverPosition.value ?: _pickupLocation.value
                    if (driverPos != null) {
                        calculateRoute(driverPos, LatLng(req.pickupLat, req.pickupLng), emptyList())
                    }
                } else if (res is Resource.Error) {
                    _actionError.value = res.message ?: "This ride was already taken"
                    _incomingRideRequest.value = null
                }
            }
        }
    }

    fun markArrived() {
        val rideId = _activeTripId.value ?: return
        viewModelScope.launch {
            repository.updateRideStatus(rideId, "ARRIVED").collect { res ->
                if (res is Resource.Success) {
                    _bookingStatus.value = BookingStatus.ARRIVED
                    _remainingTime.value = "Arrived"
                    refreshRideState()
                }
                else if (res is Resource.Error) _actionError.value = res.message
            }
        }
    }

    fun completeTrip() {
        val rideId = _activeTripId.value ?: return
        val fare = _liveFare.value
        viewModelScope.launch {
            repository.updateRideStatus(rideId, "COMPLETED").collect { res ->
                if (res is Resource.Success) {
                    val keptWeekly = protectedWeeklyTrip
                    if (protectedWeeklyTrip) pendingWeekEndedNotice = true
                    protectedWeeklyTrip = false
                    val platformFee = if (keptWeekly) 0.0 else kotlin.math.round(fare * 0.20 * 100) / 100.0
                    _completedTripSummary.value = TripSummary(
                        fare = fare,
                        distance = _routeDistance.value ?: "Trip",
                        duration = _routeDuration.value ?: "Completed",
                        passengerName = "Passenger",
                        platformFee = platformFee,
                        driverEarning = kotlin.math.round((fare - platformFee) * 100) / 100.0
                    )
                    _bookingStatus.value = BookingStatus.COMPLETED
                    _activeTripId.value = null
                    _safetyPin.value = null
                    loadFeePlan()
                    fetchTrips()
                    refreshPayments()
                } else if (res is Resource.Error) {
                    _actionError.value = res.message
                }
            }
        }
    }

    fun submitSafetyPin(pin: String) {
        val rideId = _activeTripId.value ?: return
        viewModelScope.launch {
            repository.startRide(rideId, pin).collect { res ->
                if (res is Resource.Success) {
                    _bookingStatus.value = BookingStatus.IN_PROGRESS
                    _actionError.value = null
                    
                    // Start fetching the route and updating the map now that the ride is in progress
                    val origin = _pickupLocation.value
                    val destination = _selectedDestination.value
                    if (origin != null && destination != null) {
                        calculateRoute(origin, LatLng(destination.latitude, destination.longitude), emptyList())
                    }
                } else if (res is Resource.Error) {
                    _actionError.value = res.message ?: "PIN is wrong"
                }
            }
        }
    }

    fun verifySafetyPin(pin: String): Boolean {
        submitSafetyPin(pin)
        return true
    }

    private val _payoutAccount = mutableStateOf(com.tpsk.tj.drive.data.api.DriverPayoutAccount())
    val payoutAccount: State<com.tpsk.tj.drive.data.api.DriverPayoutAccount> = _payoutAccount
    private val _payoutBanks = mutableStateOf<List<com.tpsk.tj.drive.data.api.PayoutBank>>(emptyList())
    val payoutBanks: State<List<com.tpsk.tj.drive.data.api.PayoutBank>> = _payoutBanks

    private val _registeredContacts = MutableStateFlow<List<com.tpsk.tj.chat.model.Contact>>(emptyList())
    val registeredContacts: StateFlow<List<com.tpsk.tj.chat.model.Contact>> = _registeredContacts.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.getContacts().collect { resource ->
                if (resource is Resource.Success) {
                    _registeredContacts.value = resource.data?.filter { it.isRegistered } ?: emptyList()
                }
            }
        }
    }

    fun requestPayout() {
        _actionError.value = "The full available amount is sent to your bank every Monday at 06:00. It waits if you are still on a ride."
    }

    fun loadPayoutAccount() {
        viewModelScope.launch {
            repository.getPayoutAccount().collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _payoutAccount.value = res.data
                    _availablePayout.value = res.data.available
                }
            }
            repository.getPayoutBanks().collect { res ->
                if (res is Resource.Success && res.data != null) _payoutBanks.value = res.data
            }
        }
    }

    fun savePayoutAccount(name: String, number: String, bankCode: String, bankName: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            repository.savePayoutAccount(name, number, bankCode, bankName).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _payoutAccount.value = res.data
                    _availablePayout.value = res.data.available
                    onDone("Bank account saved. The full amount pays out every Monday at 06:00.")
                } else if (res is Resource.Error) {
                    onDone(res.message ?: "Could not save bank account")
                }
            }
        }
    }

    private var pendingTopUpReference: String? = null

    fun startCardTopUp(amount: Double, onCheckout: (String) -> Unit) {
        viewModelScope.launch {
            repository.topUpWallet(amount).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    pendingTopUpReference = res.data.reference
                    val url = res.data.authorization_url
                    if (!url.isNullOrBlank()) onCheckout(url)
                    else _actionError.value = "Card checkout did not start"
                } else if (res is Resource.Error) {
                    _actionError.value = res.message
                }
            }
        }
    }

    fun confirmPendingTopUp() {
        val reference = pendingTopUpReference ?: return
        viewModelScope.launch {
            repository.confirmTopUp(reference).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    pendingTopUpReference = null
                    _walletBalance.value = res.data
                    _actionError.value = "Card payment added to your wallet"
                } else if (res is Resource.Error && res.message?.contains("not finished", ignoreCase = true) != true) {
                    _actionError.value = res.message
                }
            }
        }
    }

    fun redeemVoucher(pin: String) {
        viewModelScope.launch {
            repository.redeemVoucher(pin).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _walletBalance.value = res.data
                    _actionError.value = "1Voucher added to your wallet"
                } else if (res is Resource.Error) {
                    _actionError.value = res.message
                }
            }
        }
    }

    fun declineRideRequest() {
        val id = _incomingRideRequest.value?.id
        if (!id.isNullOrBlank()) {
            RideRequestAlerts.decline(com.tpsk.tj.common.util.ContextProvider.context, id)
        }
        _incomingRideRequest.value = null
    }

    fun cancelRide(reason: String, by: String) {
        val rideId = _activeTripId.value
        if (rideId.isNullOrBlank()) {
            _bookingStatus.value = BookingStatus.NONE
            _assignedDriver.value = null
            return
        }
        viewModelScope.launch {
            repository.cancelRide(rideId, reason, by).collect { res ->
                if (res is Resource.Success) {
                    if (protectedWeeklyTrip) pendingWeekEndedNotice = true
                    protectedWeeklyTrip = false
                    _bookingStatus.value = BookingStatus.NONE
                    _assignedDriver.value = null
                    _activeTripId.value = null
                    _safetyPin.value = null
                    _incomingRideRequest.value = null
                    clearRoute()
                    loadFeePlan()
                    fetchTrips()
                    refreshPayments()
                } else if (res is Resource.Error) {
                    _actionError.value = res.message
                }
            }
        }
    }

    fun updateVehicleInfo(info: VehicleInfo) {
        viewModelScope.launch {
            repository.updateVehicleInfo(info).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _vehicleInfo.value = res.data
                    updateApprovalStatus()
                } else if (res is Resource.Error) {
                    _actionError.value = res.message
                }
            }
        }
    }

    fun uploadVehiclePhotos(context: android.content.Context, uris: List<android.net.Uri>) {
        viewModelScope.launch {
            val room = (6 - _vehicleInfo.value.photo_urls.orEmpty().size).coerceAtLeast(0)
            if (room == 0) {
                _actionError.value = "You can add up to 6 car photos"
                return@launch
            }
            uris.take(room).forEach { uri ->
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    _actionError.value = "Could not read that photo"
                    return@forEach
                }
                if (bytes.size > 8 * 1024 * 1024) {
                    _actionError.value = "Each photo must be under 8 MB"
                    return@forEach
                }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                val part = okhttp3.MultipartBody.Part.createFormData(
                    "file",
                    "car.jpg",
                    bytes.toRequestBody(mime.toMediaTypeOrNull() ?: "image/jpeg".toMediaType())
                )
                repository.uploadVehiclePhoto(part).collect { res ->
                    if (res is Resource.Success && res.data != null) {
                        _vehicleInfo.value = res.data
                    } else if (res is Resource.Error) {
                        _actionError.value = res.message
                    }
                }
            }
            if (uris.size > room) _actionError.value = "You can add up to 6 car photos"
        }
    }

    fun uploadDriverProfilePhoto(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                _actionError.value = "Could not read that profile picture"
                return@launch
            }
            if (bytes.size > 8 * 1024 * 1024) {
                _actionError.value = "Profile picture must be under 8 MB"
                return@launch
            }
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val part = okhttp3.MultipartBody.Part.createFormData(
                "file",
                "profile.jpg",
                bytes.toRequestBody(mime.toMediaTypeOrNull() ?: "image/jpeg".toMediaType())
            )
            repository.uploadDriverProfilePhoto(part).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _vehicleInfo.value = res.data
                } else if (res is Resource.Error) {
                    _actionError.value = res.message
                }
            }
        }
    }

    fun removeVehiclePhoto(url: String) {
        viewModelScope.launch {
            repository.removeVehiclePhoto(url).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _vehicleInfo.value = res.data
                } else if (res is Resource.Error) {
                    _actionError.value = res.message
                }
            }
        }
    }

    fun uploadDocumentPhoto(context: android.content.Context, type: String, uri: android.net.Uri) {
        viewModelScope.launch {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                _actionError.value = "Could not read that photo"
                return@launch
            }
            if (bytes.size > 8 * 1024 * 1024) {
                _actionError.value = "Photo must be under 8 MB"
                return@launch
            }
            val mime = context.contentResolver.getType(uri)?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
            val part = okhttp3.MultipartBody.Part.createFormData(
                "file",
                "document.jpg",
                bytes.toRequestBody(mime.toMediaType())
            )
            repository.uploadDocumentPhoto(type, part).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _driverDocuments.value = _driverDocuments.value.filter { it.type != type } + res.data
                    updateApprovalStatus()
                    _actionError.value = "$type submitted for review"
                } else if (res is Resource.Error) {
                    _actionError.value = res.message
                }
            }
        }
    }

    fun refreshPayments() {
        loadPayoutAccount()
        viewModelScope.launch {
            repository.getWalletActivity().collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _transactions.clear()
                    _transactions.addAll(res.data.transactions)
                    _availablePayout.value = res.data.availablePayout
                    if (res.data.weekEarnings > 0) _driverEarnings.value = res.data.weekEarnings
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
        ensureRideChoices()
    }

    fun ensureRideChoices() {
        if (_outOfServiceArea.value || _rideOptions.value.isNotEmpty()) return
        _rideOptions.value = rideCatalog(routeMeters, routeSeconds, priced = routeMeters > 0 && routeSeconds > 0)
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
            _suggestions.value = listOf(
                Destination(id = "1", name = "Tembisa Hospital", address = "Flint Mazibuko St, Hospital View, Tembisa", placeId = "1"),
                Destination(id = "2", name = "Tembisa Mall", address = "Rev R.T.J. Namane Dr, Tembisa", placeId = "2"),
                Destination(id = "3", name = "Phumulani Mall", address = "Olifantsfontein Rd, Tembisa", placeId = "3"),
                Destination(id = "4", name = "Makhulong Stadium", address = "Star St, Makhulong, Tembisa", placeId = "4"),
                Destination(id = "5", name = "Ebony Park", address = "Ebony Park, Tembisa", placeId = "5"),
                Destination(id = "6", name = "Ivory Park", address = "Ivory Park, Tembisa", placeId = "6"),
                Destination(id = "7", name = "Rabie Ridge", address = "Rabie Ridge, Midrand", placeId = "7"),
                Destination(id = "8", name = "Kempton Park", address = "Kempton Park, Gauteng", placeId = "8"),
                Destination(id = "9", name = "Midrand", address = "Midrand, Gauteng", placeId = "9"),
                Destination(id = "10", name = "O.R. Tambo International Airport", address = "Kempton Park, Gauteng", placeId = "10"),
                Destination(id = "11", name = "Mall of Africa", address = "Magwa Cres, Midrand", placeId = "11"),
                Destination(id = "12", name = "Boulders Shopping Centre", address = "Old Pretoria Rd, Midrand", placeId = "12"),
                Destination(id = "13", name = "Gallagher Convention Centre", address = "Richards Dr, Midrand", placeId = "13"),
                Destination(id = "14", name = "Carlswald Lifestyle Centre", address = "Harry Galaun Dr, Midrand", placeId = "14"),
                Destination(id = "15", name = "Kyalami Grand Prix Circuit", address = "Kyalami, Midrand", placeId = "15"),
                Destination(id = "16", name = "San Ridge Square", address = "New Rd, Midrand", placeId = "16"),
                Destination(id = "17", name = "Blue Hills Shopping Centre", address = "Olifantsfontein Rd, Midrand", placeId = "17"),
                Destination(id = "18", name = "Waterfall Corner", address = "Woodmead Dr, Midrand", placeId = "18"),
                Destination(id = "19", name = "Woodmead Retail Park", address = "Waterval Cres, Woodmead", placeId = "19"),
                Destination(id = "20", name = "Sandton City", address = "Sandton, Gauteng", placeId = "20")
            ).filter { it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true) }
            return
        }

        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300)
            
            val mockSuggestions = listOf(
                Destination(id = "1", name = "Tembisa Hospital", address = "Flint Mazibuko St, Hospital View, Tembisa", placeId = "1"),
                Destination(id = "2", name = "Tembisa Mall", address = "Rev R.T.J. Namane Dr, Tembisa", placeId = "2"),
                Destination(id = "3", name = "Phumulani Mall", address = "Olifantsfontein Rd, Tembisa", placeId = "3"),
                Destination(id = "4", name = "Makhulong Stadium", address = "Star St, Makhulong, Tembisa", placeId = "4"),
                Destination(id = "5", name = "Ebony Park", address = "Ebony Park, Tembisa", placeId = "5"),
                Destination(id = "6", name = "Ivory Park", address = "Ivory Park, Tembisa", placeId = "6"),
                Destination(id = "7", name = "Rabie Ridge", address = "Rabie Ridge, Midrand", placeId = "7"),
                Destination(id = "8", name = "Kempton Park", address = "Kempton Park, Gauteng", placeId = "8"),
                Destination(id = "9", name = "Midrand", address = "Midrand, Gauteng", placeId = "9"),
                Destination(id = "10", name = "O.R. Tambo International Airport", address = "Kempton Park, Gauteng", placeId = "10"),
                Destination(id = "11", name = "Mall of Africa", address = "Magwa Cres, Midrand", placeId = "11"),
                Destination(id = "12", name = "Boulders Shopping Centre", address = "Old Pretoria Rd, Midrand", placeId = "12"),
                Destination(id = "13", name = "Gallagher Convention Centre", address = "Richards Dr, Midrand", placeId = "13"),
                Destination(id = "14", name = "Carlswald Lifestyle Centre", address = "Harry Galaun Dr, Midrand", placeId = "14"),
                Destination(id = "15", name = "Kyalami Grand Prix Circuit", address = "Kyalami, Midrand", placeId = "15"),
                Destination(id = "16", name = "San Ridge Square", address = "New Rd, Midrand", placeId = "16"),
                Destination(id = "17", name = "Blue Hills Shopping Centre", address = "Olifantsfontein Rd, Midrand", placeId = "17"),
                Destination(id = "18", name = "Waterfall Corner", address = "Woodmead Dr, Midrand", placeId = "18"),
                Destination(id = "19", name = "Woodmead Retail Park", address = "Waterval Cres, Woodmead", placeId = "19"),
                Destination(id = "20", name = "Sandton City", address = "Sandton, Gauteng", placeId = "20")
            ).filter { it.name.contains(query, ignoreCase = true) || it.address.contains(query, ignoreCase = true) }
            
            _suggestions.value = mockSuggestions
            _isSearching.value = false
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
                    reportArea(it.latitude, it.longitude)
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
        ensureRideChoices()
        viewModelScope.launch {
            val live = fetchLiveRoute(origin, destination, waypoints)
            if (live == null) {
                routeMeters = 0
                routeSeconds = 0
                _routePoints.value = emptyList()
                _trafficSegments.value = emptyList()
                _routeDistance.value = null
                _routeDuration.value = null
                _rideOptions.value = rideCatalog(0, 0, priced = false)
                _actionError.value = "Couldn't get a live traffic route. Try again."
                return@launch
            }
            if (live.meters > 100000) {
                _outOfServiceArea.value = true
                _routePoints.value = emptyList()
                _trafficSegments.value = emptyList()
                _rideOptions.value = emptyList()
                return@launch
            }
            _outOfServiceArea.value = false
            _routePoints.value = live.points
            _trafficSegments.value = emptyList()
            routeMeters = live.meters
            routeSeconds = live.seconds
            _routeDistance.value = live.distanceText
            _routeDuration.value = live.durationText
            calculateRideOptions(live.meters, live.seconds)
            refreshPickupEta(origin)
        }
    }

    private data class LiveRoute(val points: List<LatLng>, val meters: Long, val seconds: Long, val distanceText: String, val durationText: String)

    private suspend fun fetchLiveRoute(origin: LatLng, destination: LatLng, waypoints: List<LatLng>): LiveRoute? = withContext(Dispatchers.IO) {
        val allPoints = mutableListOf<LatLng>()
        allPoints.add(origin)
        allPoints.addAll(waypoints)
        allPoints.add(destination)

        val coordsString = allPoints.joinToString(";") { "${it.longitude},${it.latitude}" }
        val urlString = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/$coordsString?geometries=geojson&overview=full&access_token=$mapboxAccessToken"

        try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(urlString).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = org.json.JSONObject(responseBody)
                val routes = json.optJSONArray("routes")
                if (routes != null && routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val meters = route.optDouble("distance", 0.0).toLong()
                    val seconds = route.optDouble("duration", 0.0).toLong()

                    val geometry = route.optJSONObject("geometry")
                    val coordinates = geometry?.optJSONArray("coordinates")
                    val polylinePoints = mutableListOf<LatLng>()

                    if (coordinates != null) {
                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lng = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            polylinePoints.add(LatLng(lat, lng))
                        }
                    }

                    if (polylinePoints.isNotEmpty()) {
                        return@withContext LiveRoute(
                            points = polylinePoints,
                            meters = meters,
                            seconds = seconds,
                            distanceText = String.format(Locale.getDefault(), "%.1f km", meters / 1000.0),
                            durationText = "${((seconds + 59) / 60).coerceAtLeast(1)} min"
                        )
                    }
                }
            }
        } catch (_: Exception) { }

        // Fallback to straight line / default estimate if Mapbox call fails
        val distanceMeters = 5000L
        val durationSeconds = 600L
        LiveRoute(
            points = allPoints,
            meters = distanceMeters,
            seconds = durationSeconds,
            distanceText = "5.0 km",
            durationText = "10 min"
        )
    }

    private suspend fun trafficSeconds(origin: LatLng, destination: LatLng): Long? {
        return fetchLiveRoute(origin, destination, emptyList())?.seconds
    }

    fun clearRoute() {
        _routePoints.value = emptyList()
        _routeDistance.value = null
        _routeDuration.value = null
        routeMeters = 0
        routeSeconds = 0
        _rideOptions.value = emptyList()
        _outOfServiceArea.value = false
    }

    fun addTripStop(stop: Destination) {
        if (_tripStops.size >= 2 || stop.latitude == 0.0) return
        _tripStops.add(stop)
        refreshQuotedRoute()
    }

    fun removeTripStop(index: Int) {
        if (index !in _tripStops.indices) return
        _tripStops.removeAt(index)
        refreshQuotedRoute()
    }

    fun refreshQuotedRoute() {
        val pickup = _pickupLocation.value ?: return
        val dest = _selectedDestination.value ?: return
        if (dest.latitude == 0.0) return
        calculateRoute(pickup, LatLng(dest.latitude, dest.longitude), _tripStops.map { LatLng(it.latitude, it.longitude) })
    }

    fun watchNearbyPeople(context: Context) {
        val fine = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != android.content.pm.PackageManager.PERMISSION_GRANTED && coarse != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        if (areaJob?.isActive == true) return
        areaJob = viewModelScope.launch {
            while (isActive) {
                val loc = if (_isDriverMode.value) _driverPosition.value ?: _pickupLocation.value else _pickupLocation.value
                if (loc != null) reportArea(loc.latitude, loc.longitude)
                delay(60_000)
            }
        }
    }

    fun noteDriverPosition(lat: Double, lng: Double) {
        val point = LatLng(lat, lng)
        _driverPosition.value = point
        if (_isDriverMode.value) {
            refreshMovingEta(point)
            reportArea(lat, lng)
        }
    }

    fun markStop(index: Int, action: String) {
        val rideId = _activeTripId.value ?: return
        viewModelScope.launch {
            repository.markStop(rideId, index, action).collect { res ->
                if (res is Resource.Success) refreshRideState()
                else if (res is Resource.Error) _actionError.value = res.message
            }
        }
    }

    private fun reportArea(lat: Double, lng: Double) {
        val now = System.currentTimeMillis()
        if (now - lastAreaAt < 60_000) return
        lastAreaAt = now
        viewModelScope.launch {
            repository.reportPresence(lat, lng).collect { res ->
                if (res is Resource.Success) {
                    _areaPresence.value = res.data
                    _areaError.value = null
                } else if (res is Resource.Error) {
                    _areaError.value = res.message
                }
            }
        }
    }

    private fun refreshPickupEta(pickup: LatLng) {
        viewModelScope.launch {
            repository.getNearbyDrivers(pickup.latitude, pickup.longitude).collect { res ->
                if (res !is Resource.Success) return@collect
                val drivers = res.data.orEmpty().filter { it.last_lat != 0.0 || it.last_lng != 0.0 }
                _nearbyDrivers.value = drivers.map {
                    NearbyDriver(it.user_id, LatLng(it.last_lat, it.last_lng), 0f, name = "Driver")
                }
                val nearest = drivers.minByOrNull { distanceKm(it.last_lat, it.last_lng, pickup.latitude, pickup.longitude) }
                val label = if (nearest == null) {
                    "No drivers nearby"
                } else {
                    val seconds = trafficSeconds(LatLng(nearest.last_lat, nearest.last_lng), pickup)
                    if (seconds == null) "Updating arrival" else "${((seconds + 59) / 60).coerceAtLeast(1)} min"
                }
                _rideOptions.value = _rideOptions.value.map { it.copy(arrivalTime = label) }
            }
        }
    }

    private fun refreshMovingEta(from: LatLng) {
        val now = System.currentTimeMillis()
        if (now - lastEtaAt < 20_000) return
        val target = when (_bookingStatus.value) {
            BookingStatus.EN_ROUTE -> livePickup ?: _pickupLocation.value
            BookingStatus.IN_PROGRESS -> {
                val next = _activeStops.value.firstOrNull { it.left_at == 0L && it.lat != 0.0 }
                if (next != null) LatLng(next.lat, next.lng) else liveDest ?: _selectedDestination.value?.let { LatLng(it.latitude, it.longitude) }
            }
            else -> null
        } ?: return
        lastEtaAt = now
        viewModelScope.launch {
            val seconds = trafficSeconds(from, target)
            if (seconds != null) _remainingTime.value = "${((seconds + 59) / 60).coerceAtLeast(1)} min"
        }
    }

    private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    fun startBooking(sponsorId: String? = null) {
        val pickup = _pickupLocation.value ?: return
        val dest = _selectedRide.value ?: return
        val pickupAddr = _pickupAddress.value
        val destAddr = _selectedDestination.value?.address ?: "Destination"
        val dLat = _selectedDestination.value?.latitude ?: 0.0
        val dLng = _selectedDestination.value?.longitude ?: 0.0
        val distance = _routeDistance.value ?: return
        if (routeMeters <= 0 || routeSeconds <= 0) {
            _actionError.value = "Couldn't get a live traffic route. Try again."
            return
        }
        val stops = _tripStops.map {
            com.tpsk.tj.drive.data.api.RideStopBody(it.address.ifBlank { it.name }, it.latitude, it.longitude)
        }
        
        viewModelScope.launch {
            _bookingStatus.value = BookingStatus.SEARCHING
            _assignedDriver.value = null
            _safetyPin.value = null
            
            // If sponsorId is provided, just pass it to the existing createRideRequest
            repository.createRideRequest(
                pickup = pickupAddr,
                dest = destAddr,
                pLat = pickup.latitude,
                pLng = pickup.longitude,
                dLat = dLat,
                dLng = dLng,
                fare = dest.price.toDoubleOrNull() ?: 0.0,
                distance = distance,
                type = dest.name,
                meters = routeMeters.toInt(),
                seconds = routeSeconds.toInt(),
                stops = stops,
                sponsorId = sponsorId
            ).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    if (sponsorId != null) {
                        _bookingStatus.value = BookingStatus.NONE
                        _actionError.value = "Sponsorship requested. Waiting for approval."
                    } else if (_bookingStatus.value == BookingStatus.NONE) {
                        // User cancelled before network finished. Clean it up on server.
                        _activeTripId.value = res.data
                        cancelRide("Cancelled during request", "PASSENGER")
                    } else {
                        _activeTripId.value = res.data
                        _liveFare.value = dest.price.toDoubleOrNull() ?: 0.0
                        refreshRideState()
                    }
                } else if (res is Resource.Error) {
                    _bookingStatus.value = BookingStatus.NONE
                    _actionError.value = res.message ?: "Could not request this ride"
                }
            }
        }
    }

    fun clearTripSummary() {
        _completedTripSummary.value = null
        _bookingStatus.value = BookingStatus.NONE
    }

    fun cancelBooking() {
        if (_bookingStatus.value == BookingStatus.COMPLETED || _bookingStatus.value == BookingStatus.NONE) {
            _bookingStatus.value = BookingStatus.NONE
            _activeTripId.value = null
            return
        }
        cancelRide("User cancelled", if (_isDriverMode.value) "DRIVER" else "PASSENGER")
    }

    private fun quoteFare(meters: Long, seconds: Long, name: String): Double {
        val rate = when (name) {
            "TJ Go" -> 7.0
            "TJ Comfort" -> 10.0
            "TJ Premium" -> 18.0
            "TJ XL" -> 12.0
            else -> 8.5
        }
        val raw = 20.0 + (meters / 1000.0) * rate + (seconds / 60.0) * 1.5
        return floor(maxOf(15.0, raw) * 100 + 0.5) / 100.0
    }

    private fun calculateRideOptions(meters: Long, seconds: Long) {
        _rideOptions.value = rideCatalog(meters, seconds, priced = true)
    }

    private fun rideCatalog(meters: Long, seconds: Long, priced: Boolean): List<RideOption> {
        val names = listOf(
            RideOption("1", "TJ Go", "", arrivalTime = "Checking drivers", description = "Standard, compact rides", iconColor = androidx.compose.ui.graphics.Color(0xFF4CAF50), capacity = 4),
            RideOption("2", "TJ Comfort", "", arrivalTime = "Checking drivers", description = "Newer cars, extra legroom", iconColor = androidx.compose.ui.graphics.Color(0xFF2196F3), capacity = 4),
            RideOption("3", "TJ Premium", "", arrivalTime = "Checking drivers", description = "Luxury & top drivers", iconColor = androidx.compose.ui.graphics.Color(0xFF9C27B0), capacity = 4),
            RideOption("4", "TJ XL", "", arrivalTime = "Checking drivers", description = "SUVs for groups up to 6", iconColor = androidx.compose.ui.graphics.Color(0xFFFF9800), capacity = 6)
        )
        return names.map { option ->
            option.copy(price = if (priced) String.format(Locale.US, "%.2f", quoteFare(meters, seconds, option.name)) else "—")
        }
    }

    fun getPlaceDetails(context: Context, destination: Destination, onComplete: (Destination) -> Unit) {
        // Find the place in our mock list to get coordinates if possible, otherwise use a default coordinate
        val mockCoordinate = when(destination.id) {
            "1" -> LatLng(-25.9922, 28.2125) // Tembisa Hospital
            "2" -> LatLng(-25.9961, 28.2319) // Tembisa Mall
            "3" -> LatLng(-25.9813, 28.2198) // Phumulani Mall
            "4" -> LatLng(-26.0028, 28.2256) // Makhulong Stadium
            "5" -> LatLng(-25.9840, 28.1880) // Ebony Park
            "6" -> LatLng(-25.9870, 28.1960) // Ivory Park
            "7" -> LatLng(-26.0120, 28.1750) // Rabie Ridge
            "8" -> LatLng(-26.1030, 28.2320) // Kempton Park
            "9" -> LatLng(-25.9960, 28.1250) // Midrand
            "10" -> LatLng(-26.1360, 28.2410) // OR Tambo
            "11" -> LatLng(-26.0150, 28.1060) // Mall of Africa
            "12" -> LatLng(-25.9990, 28.1290) // Boulders
            "13" -> LatLng(-26.0020, 28.1320) // Gallagher
            "14" -> LatLng(-25.9850, 28.1180) // Carlswald
            "15" -> LatLng(-25.9980, 28.0670) // Kyalami
            "16" -> LatLng(-25.9800, 28.1250) // San Ridge
            "17" -> LatLng(-25.9520, 28.1010) // Blue Hills
            "18" -> LatLng(-26.0350, 28.0820) // Waterfall Corner
            "19" -> LatLng(-26.0460, 28.0890) // Woodmead
            "20" -> LatLng(-26.1070, 28.0530) // Sandton City
            else -> LatLng(-25.9964, 28.2268) // Default to somewhere in Tembisa
        }
        
        onComplete(destination.copy(latitude = mockCoordinate.latitude, longitude = mockCoordinate.longitude))
    }
}
