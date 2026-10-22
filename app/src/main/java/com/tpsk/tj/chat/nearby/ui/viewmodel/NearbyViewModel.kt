package com.tpsk.tj.chat.nearby.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.tpsk.tj.chat.nearby.data.NearbyRepository
import com.tpsk.tj.chat.nearby.model.NearbyPost
import com.tpsk.tj.chat.nearby.model.NearbyReply
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import java.util.Locale

data class NearbyUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val posts: List<NearbyPost> = emptyList(),
    val hasLocationPermission: Boolean = true,
    val isGpsEnabled: Boolean = true,
    val coarseAreaName: String = "Nearby",
    val userLat: Double = 0.0,
    val userLng: Double = 0.0,
    val hasLocationFix: Boolean = false,
    val radiusKm: Int = 10,
    val selectedCity: String? = null,
    val emptyReason: String? = null,
    val errorMessage: String? = null,
    val isCachedLocation: Boolean = false,
    val activeThreadPost: NearbyPost? = null,
    val showCreatePostSheet: Boolean = false
)

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val repository: NearbyRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()

    init {
        refreshLocationAndFeed()
    }

    fun refreshLocationAndFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val hasPermission = checkPermission()
            val gpsOn = isGpsProviderEnabled()

            val resolved = resolveLocation()
            if (!resolved.hasFix) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasLocationPermission = hasPermission,
                    isGpsEnabled = gpsOn,
                    coarseAreaName = "Nearby",
                    userLat = 0.0,
                    userLng = 0.0,
                    hasLocationFix = false,
                    posts = emptyList(),
                    emptyReason = if (!hasPermission) "NO_LOCATION_PERMISSION" else "NO_LOCATION"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                hasLocationPermission = hasPermission,
                isGpsEnabled = gpsOn,
                coarseAreaName = resolved.area,
                userLat = resolved.lat,
                userLng = resolved.lng,
                hasLocationFix = true
            )

            loadFeed(resolved.lat, resolved.lng, _uiState.value.radiusKm, null)
        }
    }

    private fun loadFeed(lat: Double, lng: Double, radiusKm: Int, selectedCity: String?) {
        viewModelScope.launch {
            repository.getNearbyPosts(lat, lng, radiusKm, selectedCity).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val postsList = result.data.orEmpty()
                        val reason = if (postsList.isEmpty()) {
                            if (!_uiState.value.hasLocationPermission) "NO_LOCATION_PERMISSION"
                            else "EMPTY_AREA"
                        } else {
                            null
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            posts = postsList,
                            emptyReason = reason
                        )
                        relabelPlaceholderAreas(postsList)
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                }
            }
        }
    }

    private data class ResolvedLocation(
        val lat: Double,
        val lng: Double,
        val area: String,
        val hasFix: Boolean
    )

    private suspend fun resolveLocation(): ResolvedLocation {
        val selected = _uiState.value.selectedCity?.takeIf { it.isNotBlank() }
        if (selected != null) {
            val coords = geocodeName(selected)
            if (coords != null) {
                repository.saveCoarseLocation(coords.first, coords.second, selected)
                return ResolvedLocation(coords.first, coords.second, selected, true)
            }
        }
        if (!checkPermission()) return cachedFix()
        val location = requestFreshLocation() ?: lastKnownLocation()
        if (location == null) return cachedFix()
        val area = selected ?: reverseGeocode(location.latitude, location.longitude) ?: "Nearby"
        repository.saveCoarseLocation(location.latitude, location.longitude, area)
        return ResolvedLocation(location.latitude, location.longitude, area, true)
    }

    private suspend fun cachedFix(): ResolvedLocation {
        if (!repository.hasRealCachedLocation()) {
            return ResolvedLocation(0.0, 0.0, "Nearby", false)
        }
        val lat = repository.getCachedCoarseLat()
        val lng = repository.getCachedCoarseLng()
        var area = repository.getCachedCoarseAreaName()
        if (area.equals("Braamfontein", ignoreCase = true)) {
            area = reverseGeocode(lat, lng) ?: area
            repository.saveCoarseLocation(lat, lng, area)
        }
        return ResolvedLocation(lat, lng, area, true)
    }

    private suspend fun requestFreshLocation(): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { cont ->
            try {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            } catch (_: SecurityException) {
                cont.resume(null)
            }
        }
    }

    private suspend fun lastKnownLocation(): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { cont ->
            try {
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            } catch (_: SecurityException) {
                cont.resume(null)
            }
        }
    }

    private suspend fun reverseGeocode(lat: Double, lng: Double): String? {
        val address = lookupAddress(lat, lng) ?: return null
        return formatAddress(address)
    }

    private suspend fun geocodeName(name: String): Pair<Double, Double>? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocationName(name, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        cont.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        cont.resume(null)
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocationName(name, 1)?.firstOrNull() }.getOrNull()
            }
        } ?: return null
        return address.latitude to address.longitude
    }

    private suspend fun lookupAddress(lat: Double, lng: Double): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        cont.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        cont.resume(null)
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching { geocoder.getFromLocation(lat, lng, 1)?.firstOrNull() }.getOrNull()
            }
        }
    }

    private fun relabelPlaceholderAreas(posts: List<NearbyPost>) {
        val placeholders = posts.filter {
            it.coarseAreaName.equals("Braamfontein", ignoreCase = true) && it.latitude != 0.0 && it.longitude != 0.0
        }
        if (placeholders.isEmpty()) return
        viewModelScope.launch {
            val renamed = placeholders.mapNotNull { post ->
                val area = reverseGeocode(post.latitude, post.longitude) ?: return@mapNotNull null
                if (area.equals(post.coarseAreaName, ignoreCase = true)) return@mapNotNull null
                post.id to area
            }.toMap()
            if (renamed.isEmpty()) return@launch
            val current = _uiState.value.posts
            _uiState.value = _uiState.value.copy(
                posts = current.map { post ->
                    renamed[post.id]?.let { post.copy(coarseAreaName = it) } ?: post
                }
            )
        }
    }

    private fun formatAddress(address: Address): String {
        val suburb = address.subLocality?.trim().orEmpty()
        val city = address.locality?.trim().orEmpty()
        return when {
            suburb.isNotBlank() && city.isNotBlank() && !suburb.equals(city, true) -> "$suburb, $city"
            suburb.isNotBlank() -> suburb
            city.isNotBlank() -> city
            !address.subAdminArea.isNullOrBlank() -> address.subAdminArea
            !address.adminArea.isNullOrBlank() -> address.adminArea
            else -> "Nearby"
        }
    }

    fun setRadius(radiusKm: Int) {
        _uiState.value = _uiState.value.copy(radiusKm = radiusKm)
        refreshLocationAndFeed()
    }

    fun setSelectedCity(city: String?) {
        _uiState.value = _uiState.value.copy(selectedCity = city?.takeIf { it.isNotBlank() })
        refreshLocationAndFeed()
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.likePost(postId).collect { result ->
                if (result is Resource.Success) {
                    refreshLocationAndFeed()
                }
            }
        }
    }

    fun createPost(
        content: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (!_uiState.value.hasLocationFix) {
                onError("Turn on location so Nearby can use your area")
                return@launch
            }
            val area = _uiState.value.coarseAreaName
            repository.createPost(
                content = content,
                authorId = authorId,
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                areaName = area,
                latitude = _uiState.value.userLat,
                longitude = _uiState.value.userLng
            ).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val created = result.data
                        if (created != null) {
                            _uiState.value = _uiState.value.copy(
                                posts = listOf(created) + _uiState.value.posts.filter { it.id != created.id },
                                showCreatePostSheet = false,
                                errorMessage = null
                            )
                        }
                        onSuccess()
                        refreshLocationAndFeed()
                    }
                    is Resource.Error -> {
                        onError(result.message ?: "Failed to post")
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun replyToPost(
        postId: String,
        replyContent: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.replyToPost(postId, replyContent, authorId, authorName, authorAvatarUrl).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        refreshLocationAndFeed()
                        // Update active thread post if currently open
                        val currentActive = _uiState.value.activeThreadPost
                        if (currentActive?.id == postId) {
                            val updatedReply = result.data
                            if (updatedReply != null) {
                                _uiState.value = _uiState.value.copy(
                                    activeThreadPost = currentActive.copy(
                                        replyCount = currentActive.replyCount + 1,
                                        replies = currentActive.replies + updatedReply
                                    )
                                )
                            }
                        }
                        onSuccess()
                    }
                    is Resource.Error -> onError(result.message ?: "Failed to send reply")
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun openThread(post: NearbyPost) {
        _uiState.value = _uiState.value.copy(
            activeThreadPost = post.copy(replies = emptyList()),
            isLoading = true,
            errorMessage = null
        )
        viewModelScope.launch {
            repository.getRepliesForPost(post.id).collect { res ->
                val currentActive = _uiState.value.activeThreadPost
                if (currentActive?.id != post.id) return@collect
                when (res) {
                    is Resource.Success -> {
                        val replies = res.data.orEmpty()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            activeThreadPost = currentActive.copy(
                                replyCount = replies.size,
                                replies = replies
                            )
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = res.message ?: "Could not load replies"
                        )
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun closeThread() {
        _uiState.value = _uiState.value.copy(activeThreadPost = null)
    }

    fun reportPost(postId: String) {
        repository.reportPost(postId)
        if (_uiState.value.activeThreadPost?.id == postId) {
            closeThread()
        }
        refreshLocationAndFeed()
    }

    fun blockUser(userId: String) {
        repository.blockUser(userId)
        refreshLocationAndFeed()
    }

    private fun checkPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    private fun isGpsProviderEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }
}
