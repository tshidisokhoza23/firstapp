package com.tpsk.tj.stokvel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.stokvel.data.model.NonCustodialFulfillmentResponse
import com.tpsk.tj.stokvel.data.model.RfqItem
import com.tpsk.tj.stokvel.data.model.StokvelRfq
import com.tpsk.tj.stokvel.data.model.SupplierBid
import com.tpsk.tj.stokvel.data.remote.CreateRfqRequestDto
import com.tpsk.tj.stokvel.domain.usecase.AcceptBidUseCase
import com.tpsk.tj.stokvel.domain.usecase.CreateRfqUseCase
import com.tpsk.tj.stokvel.domain.usecase.GetBidsForRfqUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RfqViewModel @Inject constructor(
    private val createRfqUseCase: CreateRfqUseCase,
    private val getBidsForRfqUseCase: GetBidsForRfqUseCase,
    private val acceptBidUseCase: AcceptBidUseCase
) : ViewModel() {

    private val _createRfqState = MutableStateFlow<Resource<StokvelRfq>?>(null)
    val createRfqState: StateFlow<Resource<StokvelRfq>?> = _createRfqState

    private val _bidsState = MutableStateFlow<Resource<List<SupplierBid>>>(Resource.Loading())
    val bidsState: StateFlow<Resource<List<SupplierBid>>> = _bidsState

    private val _acceptBidState = MutableStateFlow<Resource<NonCustodialFulfillmentResponse>?>(null)
    val acceptBidState: StateFlow<Resource<NonCustodialFulfillmentResponse>?> = _acceptBidState

    fun createRfq(
        groupId: String,
        targetRegion: String,
        itemsManifest: List<RfqItem>,
        maxBudget: Double? = null,
        deliveryPreference: String = "any"
    ) {
        viewModelScope.launch {
            val req = CreateRfqRequestDto(targetRegion, itemsManifest, maxBudget, deliveryPreference)
            createRfqUseCase(groupId, req).collect { result ->
                _createRfqState.value = result
            }
        }
    }

    fun fetchBids(groupId: String, rfqId: String) {
        viewModelScope.launch {
            getBidsForRfqUseCase(groupId, rfqId).collect { result ->
                _bidsState.value = result
            }
        }
    }

    fun acceptBid(groupId: String, rfqId: String, bidId: String) {
        viewModelScope.launch {
            acceptBidUseCase(groupId, rfqId, bidId).collect { result ->
                _acceptBidState.value = result
            }
        }
    }
}
