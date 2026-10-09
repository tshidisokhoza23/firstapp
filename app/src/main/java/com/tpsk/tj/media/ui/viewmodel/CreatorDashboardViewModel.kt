package com.tpsk.tj.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.data.api.CreatorPostStatDto
import com.tpsk.tj.common.data.api.CreatorSettingsRequest
import com.tpsk.tj.common.data.api.StudioApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

data class CommissionRequest(
    val id: String,
    val clientName: String,
    val budget: String,
    val deadline: String,
    val status: String
)

data class ActiveCommission(
    val id: String,
    val clientName: String,
    val title: String,
    val progressStage: String
)

data class CreatorDashboardUiState(
    val selectedTimeRange: String = "30D",
    val earningsThisMonth: String = "R 0.00",
    val earningsAllTime: String = "R 0.00",
    val earningsRange: String = "R 0.00",
    val tipsReceivedCount: Int = 0,
    val tipsTotalAmount: String = "R 0.00",
    val tipsCoins: Int = 0,
    val profileViews: Int = 0,
    val postViews: Int = 0,
    val newFollowers: Int = 0,
    val acceptTips: Boolean = true,
    val acceptCommissions: Boolean = false,
    val isArtist: Boolean = false,
    val loaded: Boolean = false,
    val note: String? = null,
    val posts: List<CreatorPostStatDto> = emptyList(),
    val commissionRequests: List<CommissionRequest> = emptyList(),
    val activeCommissions: List<ActiveCommission> = emptyList()
)

@HiltViewModel
class CreatorDashboardViewModel @Inject constructor(
    private val studioApi: StudioApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorDashboardUiState())
    val uiState: StateFlow<CreatorDashboardUiState> = _uiState.asStateFlow()

    init {
        load("30D")
    }

    fun selectTimeRange(range: String) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = range)
        load(range)
    }

    fun toggleAcceptTips(enabled: Boolean) {
        val next = _uiState.value.copy(acceptTips = enabled)
        _uiState.value = next
        save(next)
    }

    fun toggleAcceptCommissions(enabled: Boolean) {
        val next = _uiState.value.copy(acceptCommissions = enabled)
        _uiState.value = next
        save(next)
    }

    fun updateCommissionStatus(requestId: String, newStatus: String) {
        _uiState.value = _uiState.value.copy(note = "Commission requests are not available yet.")
    }

    private fun load(range: String) {
        viewModelScope.launch {
            try {
                val response = studioApi.dashboard(range)
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _uiState.value = _uiState.value.copy(loaded = true, note = "Could not load creator stats")
                    return@launch
                }
                _uiState.value = _uiState.value.copy(
                    selectedTimeRange = range,
                    earningsThisMonth = formatRand(body.earningsThisMonthCents),
                    earningsAllTime = formatRand(body.earningsAllTimeCents),
                    earningsRange = formatRand(body.earningsRangeCents),
                    tipsReceivedCount = body.tipsCount,
                    tipsTotalAmount = formatRand(body.tipsCents),
                    tipsCoins = body.tipsCoins,
                    profileViews = 0,
                    postViews = body.postViews,
                    newFollowers = body.newFollowers,
                    acceptTips = body.acceptTips,
                    acceptCommissions = body.acceptCommissions,
                    isArtist = body.isArtist,
                    posts = body.posts,
                    commissionRequests = emptyList(),
                    activeCommissions = emptyList(),
                    loaded = true,
                    note = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(loaded = true, note = e.message ?: "Could not load creator stats")
            }
        }
    }

    private fun save(state: CreatorDashboardUiState) {
        viewModelScope.launch {
            try {
                studioApi.updateSettings(CreatorSettingsRequest(state.acceptTips, state.acceptCommissions))
            } catch (_: Exception) {
                _uiState.value = state.copy(note = "Could not save settings")
            }
        }
    }

    private fun formatRand(cents: Int): String {
        val sign = if (cents < 0) "-" else ""
        val value = abs(cents)
        return "$sign R ${value / 100}.${(value % 100).toString().padStart(2, '0')}"
    }
}
