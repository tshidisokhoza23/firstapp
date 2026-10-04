package com.tpsk.tj.mingle.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.util.MediaUtils
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.mingle.data.api.MingleActivityDto
import com.tpsk.tj.mingle.data.api.MingleIntroDto
import com.tpsk.tj.mingle.data.api.MinglePhotoDto
import com.tpsk.tj.mingle.data.api.MingleProfileDto
import com.tpsk.tj.mingle.domain.repository.MingleRepository
import com.tpsk.tj.mingle.ui.MingleActivityNotification
import com.tpsk.tj.mingle.ui.MingleActivityType
import com.tpsk.tj.mingle.ui.MingleProfile
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tpsk.tj.vocab.domain.repository.UserRepository
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
    val nextLikeCost: Double = 0.0,
    val freeLikesRemaining: Int = 0,
    val introCost: Double = 0.0,
    val superLikeCost: Double = 0.0,
    val boostCost: Double = 0.0,
    val admirerCost: Double = 0.0,
    val extendCost: Double = 0.0,
    val profilesRemaining: Int = 0,
    val intros: List<MingleIntroDto> = emptyList(),
    val minBalance: Double = 0.0,
    val hasLocationPermission: Boolean = false,
    val hasLocation: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val error: String? = null,
    val emptyReason: String? = null,
    val needsPoints: Boolean = false,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
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
        val remaining = _uiState.value.profiles.filterNot { candidate -> excludedContactIds.contains(candidate.id) }
        if (remaining.size != _uiState.value.profiles.size || remaining.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                profiles = remaining,
                emptyReason = if (remaining.isEmpty() && _uiState.value.profiles.isNotEmpty()) "CONTACTS_HIDDEN" else _uiState.value.emptyReason
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
                            isLoading = false,
                            activities = (result.data ?: emptyList()).map { it.toNotification() }
                        )
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
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

    private var lastBackendLocation: Location? = null

    fun refreshLocationAndFeed() {
        val generation = ++feedGeneration
        viewModelScope.launch {
            refreshPermission()
            if (!_uiState.value.hasLocationPermission) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    emptyReason = "NO_LOCATION",
                    error = "Location permission is required to find people nearby"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasMore = false, emptyReason = null)
            val location = currentLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    emptyReason = "NO_LOCATION",
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
                hasLocation = true
            )

            // Battery Optimization: Update backend location only if moved >= 500 meters or first time
            val shouldUpdateBackend = lastBackendLocation == null ||
                    location.distanceTo(lastBackendLocation!!) >= 500f

            if (shouldUpdateBackend) {
                var locationUpdated = false
                var locationError: String? = null
                repository.updateLocation(lat, lng, radius).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            locationUpdated = true
                            lastBackendLocation = location
                        }
                        is Resource.Error -> locationError = result.message
                        is Resource.Loading -> Unit
                    }
                }
                if (!locationUpdated && lastBackendLocation == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        emptyReason = "NO_LOCATION",
                        error = locationError ?: "Unable to update your location"
                    )
                    return@launch
                }
            }
            nextFeedOffset = 0
            collectFeed(lat, lng, radius, offset = 0, append = false, generation = generation)
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val lat = state.latitude
        val lng = state.longitude
        if (lat == null || lng == null || state.isLoading || state.isLoadingMore || !state.hasMore) return
        val generation = feedGeneration
        viewModelScope.launch {
            collectFeed(lat, lng, state.radiusKm.roundToInt(), offset = nextFeedOffset, append = true, generation = generation)
        }
    }

    private var nextFeedOffset = 0
    private var feedGeneration = 0

    private suspend fun collectFeed(lat: Double, lng: Double, radius: Int, offset: Int, append: Boolean, generation: Int) {
        repository.getFeed(lat, lng, radius, offset = offset).collect { result ->
            if (generation != feedGeneration) return@collect
            when (result) {
                is Resource.Success -> {
                    val feed = result.data
                    val rawItems = feed?.items ?: emptyList()
                    val mapped = rawItems.mapNotNull { candidate ->
                        val candidateId = candidate.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val photos = candidate.photos?.mapNotNull { it.url?.takeIf { url -> url.isNotBlank() } } ?: emptyList()
                        if (photos.isEmpty()) return@mapNotNull null
                        MingleProfile(
                            id = candidateId,
                            name = candidate.name.orEmpty().trim().takeUnless { it.equals("Mingle user", ignoreCase = true) || it.equals("User", ignoreCase = true) }.orEmpty(),
                            age = candidate.age,
                            distanceKm = candidate.distance_km.roundToInt(),
                            bio = candidate.bio.orEmpty(),
                            photos = photos,
                            isLiked = candidate.is_liked
                        )
                    }
                    val visible = mapped.filterNot { excludedContactIds.contains(it.id) }
                    val merged = if (append) {
                        val seen = _uiState.value.profiles.map { it.id }.toSet()
                        _uiState.value.profiles + visible.filterNot { seen.contains(it.id) }
                    } else {
                        visible
                    }
                    val emptyReason = if (merged.isNotEmpty()) {
                        null
                    } else if (append) {
                        _uiState.value.emptyReason
                    } else if (mapped.isNotEmpty() && visible.isEmpty()) {
                        "CONTACTS_HIDDEN"
                    } else if (rawItems.isNotEmpty() && mapped.isEmpty()) {
                        "NO_PHOTO"
                    } else {
                        feed?.status?.takeIf { it.isNotBlank() } ?: "FEED_EXHAUSTED"
                    }
                    nextFeedOffset = offset + rawItems.size
                    val feedBalance = feed?.balance
                    if (feedBalance != null) {
                        updateUserBalance(feedBalance)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        profiles = merged,
                        emptyReason = emptyReason,
                        hasMore = feed?.has_more == true,
                        radiusKm = (feed?.radius_km ?: radius).toFloat(),
                        balance = feedBalance ?: _uiState.value.balance,
                        likeCost = feed?.like_cost ?: _uiState.value.likeCost,
                        nextLikeCost = feed?.next_like_cost ?: _uiState.value.nextLikeCost,
                        freeLikesRemaining = feed?.free_likes_remaining ?: _uiState.value.freeLikesRemaining,
                        introCost = feed?.intro_cost ?: _uiState.value.introCost,
                        superLikeCost = feed?.super_like_cost ?: _uiState.value.superLikeCost,
                        boostCost = feed?.boost_cost ?: _uiState.value.boostCost,
                        admirerCost = feed?.admirer_cost ?: _uiState.value.admirerCost,
                        extendCost = feed?.extend_cost ?: _uiState.value.extendCost,
                        profilesRemaining = feed?.free_profiles_remaining ?: _uiState.value.profilesRemaining,
                        minBalance = feed?.min_balance ?: _uiState.value.minBalance,
                        error = null
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        emptyReason = if (append) _uiState.value.emptyReason else "FEED_EXHAUSTED",
                        profiles = if (append) _uiState.value.profiles else emptyList(),
                        error = result.message
                    )
                }
                is Resource.Loading -> _uiState.value = _uiState.value.copy(
                    isLoading = if (append) _uiState.value.isLoading else true,
                    isLoadingMore = append
                )
            }
        }
    }

    fun updateUserBalance(newBalance: Double) {
        viewModelScope.launch {
            val user = userRepository.observeCurrentUser().firstOrNull()
            if (user != null) {
                userRepository.updateUserLocally(user.copy(walletBalance = newBalance))
                com.tpsk.tj.chat.rewards.data.RewardsManager.onBalanceUpdate(newBalance)
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
                com.tpsk.tj.chat.rewards.data.RewardsManager.addOptimisticReward(-amount)
                _uiState.value = _uiState.value.copy(balance = newBalance)
            }
        }
    }

    private data class TrackedSwipe(val profile: MingleProfile, val action: String, val confirmedOnServer: Boolean)

    private var lastSwipe: TrackedSwipe? = null
    private val pendingSwipes = mutableListOf<com.tpsk.tj.mingle.data.api.MingleSwipeSyncItemDto>()
    private val pendingProfiles = mutableMapOf<String, MingleProfile>()
    private val locallyCancelled = mutableSetOf<String>()

    private fun restoreProfile(profile: MingleProfile) {
        if (_uiState.value.profiles.any { it.id == profile.id }) return
        _uiState.value = _uiState.value.copy(profiles = listOf(profile) + _uiState.value.profiles)
    }

    fun extendFeed(method: String) {
        viewModelScope.launch {
            repository.extendFeed(method).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val data = result.data
                        _uiState.value = _uiState.value.copy(
                            balance = data?.balance ?: _uiState.value.balance,
                            profilesRemaining = data?.free_profiles_remaining ?: _uiState.value.profilesRemaining,
                            needsPoints = false,
                            error = null
                        )
                        refreshLocationAndFeed()
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        needsPoints = result.message == "INSUFFICIENT_POINTS",
                        error = result.message
                    )
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun boostProfile() {
        viewModelScope.launch {
            repository.boost().collect { result ->
                when (result) {
                    is Resource.Success -> _uiState.value = _uiState.value.copy(
                        balance = result.data?.balance ?: _uiState.value.balance,
                        error = null
                    )
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        needsPoints = result.message == "INSUFFICIENT_POINTS",
                        error = result.message
                    )
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun unlockAdmirer(actorId: String) {
        if (actorId.isBlank()) return
        viewModelScope.launch {
            repository.unlockAdmirer(actorId).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            balance = result.data?.balance ?: _uiState.value.balance,
                            error = null
                        )
                        loadActivity()
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        needsPoints = result.message == "INSUFFICIENT_POINTS",
                        error = result.message
                    )
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun rewind(onRewound: () -> Unit = {}) {
        val last = lastSwipe
        if (last == null) {
            _uiState.value = _uiState.value.copy(error = "Nothing to rewind")
            return
        }
        if (!last.confirmedOnServer) {
            locallyCancelled.add(last.profile.id)
            pendingSwipes.removeAll { it.target_user_id == last.profile.id }
            pendingProfiles.remove(last.profile.id)
            lastSwipe = null
            restoreProfile(last.profile)
            onRewound()
            return
        }
        viewModelScope.launch {
            repository.rewind().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val data = result.data
                        val targetId = data?.target_user_id
                        if (data != null && data.rewound && (targetId.isNullOrBlank() || targetId == last.profile.id)) {
                            lastSwipe = null
                            restoreProfile(last.profile)
                            _uiState.value = _uiState.value.copy(balance = data.balance)
                            updateUserBalance(data.balance)
                            onRewound()
                        } else if (data?.rewound == true) {
                            lastSwipe = null
                            _uiState.value = _uiState.value.copy(error = "Rewound a different swipe. Refreshing your deck.")
                            refreshLocationAndFeed()
                        } else {
                            _uiState.value = _uiState.value.copy(error = "Nothing to rewind")
                        }
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun flushPendingSwipes() {
        if (pendingSwipes.isEmpty()) return
        val batch = pendingSwipes.toList()
        viewModelScope.launch {
            repository.syncSwipes(batch).collect { result ->
                if (result !is Resource.Success) return@collect
                val results = result.data?.results.orEmpty()
                val returned = results.associateBy { it.target_user_id }
                var needsPoints = false
                pendingSwipes.removeAll { item ->
                    val status = returned[item.target_user_id]?.status ?: return@removeAll false
                    when (status) {
                        "INSUFFICIENT_POINTS" -> {
                            needsPoints = true
                            pendingProfiles.remove(item.target_user_id)?.let { restoreProfile(it) }
                            true
                        }
                        "RETRYABLE" -> false
                        else -> {
                            pendingProfiles.remove(item.target_user_id)
                            true
                        }
                    }
                }
                if (needsPoints) {
                    _uiState.value = _uiState.value.copy(needsPoints = true)
                }
                result.data?.let { updateUserBalance(it.balance) }
            }
        }
    }

    fun swipe(profile: MingleProfile, action: String, candidateName: String? = null, onMatched: (String) -> Unit, onInsufficientPoints: () -> Unit) {
        lastSwipe = TrackedSwipe(profile, action, confirmedOnServer = false)
        locallyCancelled.remove(profile.id)
        _uiState.value = _uiState.value.copy(
            profiles = _uiState.value.profiles.filterNot { it.id == profile.id }
        )
        viewModelScope.launch {
            val nameToUse = candidateName ?: profile.name
            repository.swipe(profile.id, action, candidateName = nameToUse).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        if (locallyCancelled.remove(profile.id)) {
                            repository.rewind().collect { }
                            restoreProfile(profile)
                            return@collect
                        }
                        lastSwipe = TrackedSwipe(profile, action, confirmedOnServer = true)
                        pendingSwipes.removeAll { it.target_user_id == profile.id }
                        pendingProfiles.remove(profile.id)
                        val swipe = result.data
                        if (swipe != null) {
                            updateUserBalance(swipe.balance)
                        }
                        _uiState.value = _uiState.value.copy(
                            balance = swipe?.balance ?: _uiState.value.balance,
                            freeLikesRemaining = swipe?.free_likes_remaining ?: _uiState.value.freeLikesRemaining,
                            nextLikeCost = swipe?.next_like_cost ?: _uiState.value.nextLikeCost,
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
                        if (locallyCancelled.remove(profile.id)) {
                            restoreProfile(profile)
                            return@collect
                        }
                        when (result.message) {
                            "INSUFFICIENT_POINTS" -> {
                                lastSwipe = null
                                restoreProfile(profile)
                                _uiState.value = _uiState.value.copy(needsPoints = true)
                                onInsufficientPoints()
                            }
                            "RETRYABLE" -> {
                                pendingSwipes.removeAll { it.target_user_id == profile.id }
                                pendingSwipes.add(com.tpsk.tj.mingle.data.api.MingleSwipeSyncItemDto(target_user_id = profile.id, action = action))
                                pendingProfiles[profile.id] = profile
                            }
                            else -> {
                                lastSwipe = null
                                restoreProfile(profile)
                                _uiState.value = _uiState.value.copy(error = result.message)
                            }
                        }
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun loadIntros() {
        viewModelScope.launch {
            repository.listIntros().collect { result ->
                if (result is Resource.Success) {
                    _uiState.value = _uiState.value.copy(intros = result.data.orEmpty())
                }
            }
        }
    }

    fun sendIntro(recipientId: String, message: String, onSent: () -> Unit, onNeedPoints: () -> Unit) {
        viewModelScope.launch {
            repository.sendIntro(recipientId, message).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.balance?.let { updateUserBalance(it) }
                        _uiState.value = _uiState.value.copy(error = null)
                        onSent()
                    }
                    is Resource.Error -> {
                        if (result.message == "INSUFFICIENT_POINTS") {
                            _uiState.value = _uiState.value.copy(needsPoints = true)
                            onNeedPoints()
                        } else {
                            _uiState.value = _uiState.value.copy(error = result.message)
                        }
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun respondIntro(intro: MingleIntroDto, action: String, onAccepted: (String) -> Unit) {
        val introId = intro.id ?: return
        viewModelScope.launch {
            repository.respondIntro(introId, action, intro.sender_name).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            intros = _uiState.value.intros.filterNot { it.id == introId }
                        )
                        val conversationId = result.data?.conversation_id
                        if (action == "accept" && !conversationId.isNullOrBlank()) {
                            onAccepted(conversationId)
                        }
                    }
                    is Resource.Error -> _uiState.value = _uiState.value.copy(error = result.message)
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
                // Optimistic Local Preview: Add local Uri string immediately so user sees their photo
                val localId = "local_${System.currentTimeMillis()}_${uri.hashCode()}"
                val localPhotoDto = MinglePhotoDto(id = localId, url = uri.toString())
                val optimisticList = _uiState.value.myPhotos.toMutableList()
                optimisticList.add(localPhotoDto)
                _uiState.value = _uiState.value.copy(myPhotos = optimisticList)

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
                                    currentList.removeAll { it.id == localId }
                                    if (currentList.none { it.id == photoDto.id || (it.url != null && it.url == photoDto.url) }) {
                                        currentList.add(photoDto)
                                        _uiState.value = _uiState.value.copy(myPhotos = currentList)
                                    }
                                }
                            }
                            is Resource.Error -> {
                                lastError = result.message
                                val currentList = _uiState.value.myPhotos.toMutableList()
                                currentList.removeAll { it.id == localId }
                                _uiState.value = _uiState.value.copy(myPhotos = currentList)
                            }
                            is Resource.Loading -> Unit
                        }
                    }
                } else {
                    if (lastError == null) lastError = "Unable to prepare image file for upload"
                    val currentList = _uiState.value.myPhotos.toMutableList()
                    currentList.removeAll { it.id == localId }
                    _uiState.value = _uiState.value.copy(myPhotos = currentList)
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
            myAge = profile.age,
            myGender = profile.gender.orEmpty().ifBlank { "Other" },
            myLookingFor = profile.looking_for.orEmpty().ifBlank { "Everyone" },
            myTargetAgeRange = profile.target_age_range.orEmpty().ifBlank { "18 - 35" }
        )
    }

    private suspend fun currentLocation(): Location? = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        val maxAgeMs = 2 * 60 * 1000L
        fun freshEnough(loc: Location?) = loc != null && System.currentTimeMillis() - loc.time <= maxAgeMs
        try {
            client.lastLocation.addOnSuccessListener { last ->
                if (!cont.isActive) return@addOnSuccessListener
                if (freshEnough(last)) {
                    cont.resume(last)
                    return@addOnSuccessListener
                }
                val token = CancellationTokenSource()
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                    .addOnSuccessListener { current -> if (cont.isActive) cont.resume(current ?: last) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(last) }
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
        "message_request" -> MingleActivityType.MESSAGE_REQUEST
        else -> MingleActivityType.MINGLE_LIKE
    }
    val actionText = when (mappedType) {
        MingleActivityType.MATCH -> "matched with you! Say hi 👋"
        MingleActivityType.VIEW -> "viewed your Mingle profile 👁"
        MingleActivityType.PHOTO_LIKE -> "liked a photo on your profile 📸"
        MingleActivityType.SUPER_LIKE -> "sent you a Super Like ⭐"
        MingleActivityType.MESSAGE_REQUEST -> "You received a new message request on Mingle"
        MingleActivityType.MINGLE_LIKE -> "liked your Mingle profile ❤️"
    }
    return MingleActivityNotification(
        id = id.orEmpty(),
        actorId = actor_id.orEmpty(),
        actorName = if (locked) "" else actor_name.orEmpty(),
        actorAvatarUrl = actor_avatar_url ?: photo_url,
        type = mappedType,
        actionText = actionText,
        timeAgo = created_at.toRelativeTime(),
        photoUrl = photo_url,
        conversationId = conversation_id,
        locked = locked
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
