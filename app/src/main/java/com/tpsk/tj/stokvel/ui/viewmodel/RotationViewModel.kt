package com.tpsk.tj.stokvel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.stokvel.data.model.StokvelGroup
import com.tpsk.tj.stokvel.data.model.StokvelMember
import com.tpsk.tj.stokvel.data.model.SwapRequest
import com.tpsk.tj.stokvel.domain.usecase.AdvanceCycleUseCase
import com.tpsk.tj.stokvel.domain.usecase.ConfirmTurnSwapUseCase
import com.tpsk.tj.stokvel.domain.usecase.RequestTurnSwapUseCase
import com.tpsk.tj.stokvel.domain.usecase.ShuffleOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RotationViewModel @Inject constructor(
    private val advanceCycleUseCase: AdvanceCycleUseCase,
    private val shuffleOrderUseCase: ShuffleOrderUseCase,
    private val requestTurnSwapUseCase: RequestTurnSwapUseCase,
    private val confirmTurnSwapUseCase: ConfirmTurnSwapUseCase
) : ViewModel() {

    private val _advanceCycleState = MutableStateFlow<Resource<StokvelGroup>?>(null)
    val advanceCycleState: StateFlow<Resource<StokvelGroup>?> = _advanceCycleState

    private val _shuffleState = MutableStateFlow<Resource<List<StokvelMember>>?>(null)
    val shuffleState: StateFlow<Resource<List<StokvelMember>>?> = _shuffleState

    private val _swapRequestState = MutableStateFlow<Resource<SwapRequest>?>(null)
    val swapRequestState: StateFlow<Resource<SwapRequest>?> = _swapRequestState

    private val _swapConfirmState = MutableStateFlow<Resource<Unit>?>(null)
    val swapConfirmState: StateFlow<Resource<Unit>?> = _swapConfirmState

    fun advanceCycle(groupId: String) {
        viewModelScope.launch {
            advanceCycleUseCase(groupId).collect { result ->
                _advanceCycleState.value = result
            }
        }
    }

    fun shuffleOrder(groupId: String) {
        viewModelScope.launch {
            shuffleOrderUseCase(groupId).collect { result ->
                _shuffleState.value = result
            }
        }
    }

    fun requestSwap(groupId: String, requesterMemberId: String, targetMemberId: String) {
        viewModelScope.launch {
            requestTurnSwapUseCase(groupId, requesterMemberId, targetMemberId).collect { result ->
                _swapRequestState.value = result
            }
        }
    }

    fun confirmSwap(groupId: String, swapRequestId: String, actorMemberId: String) {
        viewModelScope.launch {
            confirmTurnSwapUseCase(groupId, swapRequestId, actorMemberId).collect { result ->
                _swapConfirmState.value = result
            }
        }
    }
}
