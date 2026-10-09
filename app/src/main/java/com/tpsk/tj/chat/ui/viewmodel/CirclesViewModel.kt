package com.tpsk.tj.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.data.api.CreateCircleRequest
import com.tpsk.tj.common.data.api.StudioApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CircleItem(
    val id: String,
    val name: String,
    val memberCount: Int,
    val avatarUrls: List<String> = emptyList(),
    val isTemporary: Boolean = false,
    val expiresInfo: String? = null
)

@HiltViewModel
class CirclesViewModel @Inject constructor(
    private val studioApi: StudioApi
) : ViewModel() {

    private val _circles = MutableStateFlow<List<CircleItem>>(emptyList())
    val circles: StateFlow<List<CircleItem>> = _circles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                val response = studioApi.listCircles()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _circles.value = body.circles.map { circle ->
                        CircleItem(
                            id = circle.id,
                            name = circle.name,
                            memberCount = circle.memberCount,
                            isTemporary = circle.isTemporary,
                            expiresInfo = circle.expiresAt?.let { "Temporary • expires $it" }
                        )
                    }
                    _message.value = if (_circles.value.isEmpty()) "No circles yet. Create one to share with a real group." else null
                } else {
                    _circles.value = emptyList()
                    _message.value = "Could not load circles"
                }
            } catch (e: Exception) {
                _circles.value = emptyList()
                _message.value = e.message ?: "Could not load circles"
            }
        }
    }

    fun createCircle(name: String, isTemporary: Boolean = false) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            try {
                val response = studioApi.createCircle(CreateCircleRequest(clean, isTemporary))
                if (!response.isSuccessful) {
                    _message.value = "Could not create circle"
                    return@launch
                }
                refresh()
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not create circle"
            }
        }
    }

    fun deleteCircle(id: String) {
        viewModelScope.launch {
            try {
                studioApi.deleteCircle(id)
                refresh()
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not delete circle"
            }
        }
    }
}
