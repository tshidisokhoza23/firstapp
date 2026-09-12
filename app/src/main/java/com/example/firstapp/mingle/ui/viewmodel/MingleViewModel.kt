package com.example.firstapp.mingle.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.util.MediaUtils
import com.example.firstapp.common.util.Resource
import com.example.firstapp.mingle.data.api.MingleActivityDto
import com.example.firstapp.mingle.data.api.MinglePhotoDto
import com.example.firstapp.mingle.data.api.MingleProfileDto
import com.example.firstapp.mingle.domain.repository.MingleRepository
import com.example.firstapp.mingle.ui.MingleActivityNotification
import com.example.firstapp.mingle.ui.MingleActivityType
import com.example.firstapp.mingle.ui.MingleProfile
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.math.roundToInt

data class MingleUiState(
    val isLoading: Boolean = false,
    val profiles: List<MingleProfile> = emptyList(),
    val myPhotos: List<MinglePhotoDto> = emptyList(),
    val activities: List<MingleActivityNotification> = emptyList(),
    val radiusKm: Float = 25f,
    val balance: Double = 0.0,
    val likeCost: Double = 10.0,
    val hasLocationPermission: Boolean = false,
    val hasLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val error: String? = null,
    val needsPoints: Boolean = false,
    val lastMatchConversationId: String? = null,
    val lastMatchName: String? = null
)

@HiltViewModel
class MingleViewModel @Inject constructor(
    private val repository: MingleRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MingleUiState())
    val uiState: StateFlow<MingleUiState> = _uiState.asStateFlow()

    init {
        refreshPermission()
        loadProfile()
        if (_uiState.value.hasLocationPermission) {
            refreshLocationAndFeed()
        }
    }

    fun refreshPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        _uiState.value = _uiState.value.copy(hasLocationPermission = granted)
    }

    fun loadProfile() {
        viewModelScope.launch {
            repository.getMe().collect { result ->
                when (result) {
                    is Resource.Success -> result.data?.let { applyProfile(it) }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun loadActivity() {
        viewModelScope.launch {
            repository.getActivity().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            activities = (result.data ?: emptyList()).map { it.toNotification() }
                        )
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun setRadius(radiusKm: Float) {
        _uiState.value = _uiState.value.copy(radiusKm = radiusKm.coerceIn(1f, 100f))
    }

    fun applyRadiusAndReload() {
        refreshLocationAndFeed()
    }

    fun refreshLocationAndFeed() {
        viewModelScope.launch {
            refreshPermission()
            if (!_uiState.value.hasLocationPermission) {
                _uiState.value = _uiState.value.copy(error = "Location permission is required to find people nearby")
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val location = currentLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Unable to read your location")
                return@launch
            }
            val radius = _uiState.value.radiusKm.roundToInt()
            _uiState.value = _uiState.value.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                hasLocation = true
            )
            repository.updateLocation(location.latitude, location.longitude, radius).collect { }
            repository.getFeed(location.latitude, location.longitude, radius).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val feed = result.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            profiles = (feed?.items ?: emptyList()).map { candidate ->
                                MingleProfile(
                                    id = candidate.id.orEmpty(),
                                    name = candidate.name.orEmpty().ifBlank { "Mingle user" },
                                    age = candidate.age,
                                    distanceKm = candidate.distance_km.roundToInt(),
                                    bio = candidate.bio.orEmpty(),
                                    photos = candidate.photos?.mapNotNull { it.url } ?: emptyList(),
                                    isLiked = candidate.is_liked
                                )
                            },
                            radiusKm = (feed?.radius_km ?: radius).toFloat(),
                            balance = feed?.balance ?: _uiState.value.balance,
                            likeCost = feed?.like_cost ?: 10.0,
                            error = null
                        )
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun swipe(profile: MingleProfile, action: String, onMatched: (String) -> Unit, onInsufficientPoints: () -> Unit) {
        viewModelScope.launch {
            repository.swipe(profile.id, action).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val swipe = result.data
                        _uiState.value = _uiState.value.copy(
                            profiles = _uiState.value.profiles.filterNot { it.id == profile.id },
                            balance = swipe?.balance ?: _uiState.value.balance,
                            needsPoints = false
                        )
                        val conversationId = swipe?.conversation_id
                        if (swipe?.matched == true && !conversationId.isNullOrBlank()) {
                            _uiState.value = _uiState.value.copy(
                                lastMatchConversationId = conversationId,
                                lastMatchName = profile.name
                            )
                            onMatched(conversationId)
                        }
                    }
                    is Resource.Error -> {
                        if (result.message == "INSUFFICIENT_POINTS") {
                            _uiState.value = _uiState.value.copy(needsPoints = true)
                            onInsufficientPoints()
                        } else {
                            _uiState.value = _uiState.value.copy(error = result.message)
                        }
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun openChat(userId: String, onOpened: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.openChat(userId).collect { result ->
                when (result) {
                    is Resource.Success -> result.data?.let(onOpened)
                    is Resource.Error -> onError(result.message ?: "Match required before chatting")
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun uploadPhotos(uris: List<Uri>) {
        viewModelScope.launch {
            uris.take(5).forEach { uri ->
                val file = MediaUtils.compressImage(context, uri)
                repository.uploadPhoto(file).collect { result ->
                    when (result) {
                        is Resource.Success -> loadProfile()
                        is Resource.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                        is Resource.Loading -> Unit
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearPointsGate() {
        _uiState.value = _uiState.value.copy(needsPoints = false)
    }

    private fun applyProfile(profile: MingleProfileDto) {
        _uiState.value = _uiState.value.copy(
            myPhotos = profile.photos ?: emptyList(),
            radiusKm = profile.max_distance_km.toFloat().coerceIn(1f, 100f),
            hasLocation = profile.has_location
        )
    }

    private suspend fun currentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        try {
            client.lastLocation.addOnSuccessListener { last ->
                if (last != null) {
                    if (cont.isActive) cont.resume(last)
                    return@addOnSuccessListener
                }
                val token = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
                    .addOnSuccessListener { current -> if (cont.isActive) cont.resume(current) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                cont.invokeOnCancellation { token.cancel() }
            }.addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
        } catch (_: SecurityException) {
            if (cont.isActive) cont.resume(null)
        }
    }
}

private fun MingleActivityDto.toNotification(): MingleActivityNotification {
    val mappedType = when (type) {
        "match" -> MingleActivityType.MATCH
        "view" -> MingleActivityType.VIEW
        "photo_like" -> MingleActivityType.PHOTO_LIKE
        "super_like" -> MingleActivityType.SUPER_LIKE
        else -> MingleActivityType.MINGLE_LIKE
    }
    val actionText = when (mappedType) {
        MingleActivityType.MATCH -> "matched with you! Say hi 👋"
        MingleActivityType.VIEW -> "viewed your Mingle profile 👁"
        MingleActivityType.PHOTO_LIKE -> "liked a photo on your profile 📸"
        MingleActivityType.SUPER_LIKE -> "sent you a Super Like ⭐"
        MingleActivityType.MINGLE_LIKE -> "liked your Mingle profile ❤️"
    }
    return MingleActivityNotification(
        id = id.orEmpty(),
        actorId = actor_id.orEmpty(),
        actorName = actor_name.orEmpty().ifBlank { "Someone" },
        actorAvatarUrl = actor_avatar_url ?: photo_url,
        type = mappedType,
        actionText = actionText,
        timeAgo = created_at.toRelativeTime(),
        photoUrl = photo_url
    )
}

private fun Long.toRelativeTime(): String {
    if (this <= 0) return "just now"
    val now = System.currentTimeMillis()
    val then = if (this < 1_000_000_000_000L) this * 1000 else this
    val minutes = TimeUnit.MILLISECONDS.toMinutes(now - then)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 1440 -> "${minutes / 60}h ago"
        else -> "${minutes / 1440}d ago"
    }
}
