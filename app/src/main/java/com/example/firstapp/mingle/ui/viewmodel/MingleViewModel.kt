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
import com.example.firstapp.vocab.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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
    val lastMatchName: String? = null,
    val myName: String = "",
    val myBio: String = "",
    val myAge: Int = 25,
    val myGender: String = "Other",
    val myLookingFor: String = "Everyone",
    val myTargetAgeRange: String = "18 - 35"
) {
    val hasUploadedPhotos: Boolean get() = myPhotos.any { !it.url.isNullOrBlank() }
}

@HiltViewModel
class MingleViewModel @Inject constructor(
    private val repository: MingleRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MingleUiState())
    val uiState: StateFlow<MingleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        balance = user.walletBalance
                    )
                }
            }
        }
        refreshPermission()
        loadProfile()
        if (_uiState.value.hasLocationPermission) {
            refreshLocationAndFeed()
        }
    }

    private var excludedContactIds: Set<String> = emptySet()

    fun setExcludedContactIds(ids: Set<String>) {
        excludedContactIds = ids
        if (_uiState.value.profiles.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                profiles = _uiState.value.profiles.filterNot { candidate -> excludedContactIds.contains(candidate.id) }
            )
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
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Location permission is required to find people nearby"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val location = currentLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unable to read your location"
                )
                return@launch
            }
            val lat = location.latitude
            val lng = location.longitude
            val radius = _uiState.value.radiusKm.roundToInt()
            
            _uiState.value = _uiState.value.copy(
                latitude = lat,
                longitude = lng,
                hasLocation = location != null
            )
            var locationUpdated = false
            var locationError: String? = null
            repository.updateLocation(lat, lng, radius).collect { result ->
                when (result) {
                    is Resource.Success -> locationUpdated = true
                    is Resource.Error -> locationError = result.message
                    is Resource.Loading -> Unit
                }
            }
            if (!locationUpdated) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = locationError ?: "Unable to update your location"
                )
                return@launch
            }
            repository.getFeed(lat, lng, radius).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val feed = result.data
                        val targetRange = _uiState.value.myTargetAgeRange
                        val rawItems = feed?.items ?: emptyList()
                        val candidatesList = rawItems.mapNotNull { candidate ->
                            val candidateId = candidate.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            MingleProfile(
                                id = candidateId,
                                name = candidate.name.orEmpty().ifBlank { "Mingle user" },
                                age = candidate.age,
                                distanceKm = candidate.distance_km.roundToInt(),
                                bio = candidate.bio.orEmpty(),
                                photos = candidate.photos?.mapNotNull { it.url } ?: emptyList(),
                                isLiked = candidate.is_liked
                            )
                        }

                        val nonExcluded = candidatesList.filterNot { candidate ->
                            excludedContactIds.contains(candidate.id)
                        }
                        val filteredItems = nonExcluded.filter { candidate ->
                            isAgeInRange(candidate.age, targetRange)
                        }

                        val feedBalance = feed?.balance
                        if (feedBalance != null) {
                            updateUserBalance(feedBalance)
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            profiles = filteredItems,
                            radiusKm = (feed?.radius_km ?: radius).toFloat(),
                            balance = feedBalance ?: _uiState.value.balance,
                            likeCost = feed?.like_cost ?: 10.0,
                            error = null
                        )
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            profiles = emptyList(),
                            error = result.message
                        )
                    }
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun updateUserBalance(newBalance: Double) {
        viewModelScope.launch {
            val user = userRepository.observeCurrentUser().firstOrNull()
            if (user != null) {
                userRepository.updateUserLocally(user.copy(walletBalance = newBalance))
                com.example.firstapp.chat.rewards.data.RewardsManager.onBalanceUpdate(newBalance)
                _uiState.value = _uiState.value.copy(balance = newBalance)
            }
        }
    }

    fun deductLikePoints(amount: Double = 10.0) {
        viewModelScope.launch {
            val user = userRepository.observeCurrentUser().firstOrNull()
            if (user != null) {
                val newBalance = (user.walletBalance - amount).coerceAtLeast(0.0)
                val updatedUser = user.copy(walletBalance = newBalance)
                userRepository.updateUserLocally(updatedUser)
                com.example.firstapp.chat.rewards.data.RewardsManager.addOptimisticReward(-amount)
                _uiState.value = _uiState.value.copy(balance = newBalance)
            }
        }
    }

    fun swipe(profile: MingleProfile, action: String, candidateName: String? = null, onMatched: (String) -> Unit, onInsufficientPoints: () -> Unit) {
        viewModelScope.launch {
            val nameToUse = candidateName ?: profile.name
            repository.swipe(profile.id, action, candidateName = nameToUse).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val swipe = result.data
                        if (swipe != null) {
                            updateUserBalance(swipe.balance)
                        }
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

    fun openChat(userId: String, candidateName: String? = null, onOpened: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.openChat(userId, candidateName = candidateName).collect { result ->
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
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            var successCount = 0
            var lastError: String? = null

            for (uri in uris.take(5)) {
                val file = try {
                    MediaUtils.compressImage(context, uri)
                } catch (e: Exception) {
                    lastError = e.message ?: "Failed to process image file"
                    null
                }

                if (file != null && file.exists()) {
                    repository.uploadPhoto(file).collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                successCount++
                                result.data?.let { photoDto ->
                                    val currentList = _uiState.value.myPhotos.toMutableList()
                                    if (currentList.none { it.id == photoDto.id || (it.url != null && it.url == photoDto.url) }) {
                                        currentList.add(photoDto)
                                        _uiState.value = _uiState.value.copy(myPhotos = currentList)
                                    }
                                }
                            }
                            is Resource.Error -> {
                                lastError = result.message
                            }
                            is Resource.Loading -> Unit
                        }
                    }
                } else if (lastError == null) {
                    lastError = "Unable to prepare image file for upload"
                }
            }

            _uiState.value = _uiState.value.copy(isLoading = false)
            loadProfile()

            if (successCount == 0 && lastError != null) {
                _uiState.value = _uiState.value.copy(error = lastError)
            }
        }
    }

    fun updateProfile(name: String, bio: String, age: Int, gender: String, lookingFor: String, targetAgeRange: String = "18 - 35", onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                myName = name,
                myBio = bio,
                myAge = age,
                myGender = gender,
                myLookingFor = lookingFor,
                myTargetAgeRange = targetAgeRange
            )
            repository.updateProfile(name, bio, age, gender, lookingFor, targetAgeRange).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { applyProfile(it) }
                        onComplete()
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(error = result.message)
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun deletePhoto(photoId: String) {
        viewModelScope.launch {
            repository.deletePhoto(photoId).collect { result ->
                when (result) {
                    is Resource.Success -> loadProfile()
                    is Resource.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    is Resource.Loading -> Unit
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
        val photosToUse = if (!profile.photos.isNullOrEmpty()) {
            profile.photos
        } else if (_uiState.value.myPhotos.isNotEmpty()) {
            _uiState.value.myPhotos
        } else {
            emptyList()
        }
        _uiState.value = _uiState.value.copy(
            myPhotos = photosToUse,
            radiusKm = profile.max_distance_km.toFloat().coerceIn(1f, 100f),
            hasLocation = profile.has_location,
            myName = profile.name.orEmpty(),
            myBio = profile.bio.orEmpty(),
            myAge = if (profile.age > 0) profile.age else 25,
            myGender = profile.gender.orEmpty().ifBlank { "Other" },
            myLookingFor = profile.looking_for.orEmpty().ifBlank { "Everyone" },
            myTargetAgeRange = profile.target_age_range.orEmpty().ifBlank { "18 - 35" }
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

    private fun isAgeInRange(age: Int, rangeStr: String): Boolean {
        if (age <= 0) return true
        if (rangeStr.contains("18+")) return age >= 18
        val parts = rangeStr.split("-").mapNotNull { it.trim().filter { c -> c.isDigit() }.toIntOrNull() }
        if (parts.size == 2) {
            val minAge = parts[0]
            val maxAge = parts[1]
            return age in minAge..maxAge
        }
        return true
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
