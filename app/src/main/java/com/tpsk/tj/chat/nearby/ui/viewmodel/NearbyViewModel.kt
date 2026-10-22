package com.tpsk.tj.chat.nearby.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NearbyUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val posts: List<NearbyPost> = emptyList(),
    val hasLocationPermission: Boolean = true,
    val isGpsEnabled: Boolean = true,
    val coarseAreaName: String = "Braamfontein",
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

            val areaName = _uiState.value.selectedCity ?: repository.getCachedCoarseAreaName()
            val lat = repository.getCachedCoarseLat()
            val lng = repository.getCachedCoarseLng()

            _uiState.value = _uiState.value.copy(
                hasLocationPermission = hasPermission,
                isGpsEnabled = gpsOn,
                coarseAreaName = areaName
            )

            loadFeed(lat, lng, _uiState.value.radiusKm, _uiState.value.selectedCity)
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

    fun setRadius(radiusKm: Int) {
        _uiState.value = _uiState.value.copy(radiusKm = radiusKm)
        refreshLocationAndFeed()
    }

    fun setSelectedCity(city: String?) {
        _uiState.value = _uiState.value.copy(selectedCity = city)
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
            repository.createPost(content, authorId, authorName, authorAvatarUrl).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        refreshLocationAndFeed()
                        onSuccess()
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
        _uiState.value = _uiState.value.copy(activeThreadPost = post)
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
