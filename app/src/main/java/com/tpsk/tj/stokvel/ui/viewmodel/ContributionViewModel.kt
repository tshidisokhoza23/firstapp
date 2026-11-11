package com.tpsk.tj.stokvel.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.stokvel.data.model.PaymentMethod
import com.tpsk.tj.stokvel.data.model.StokvelContribution
import com.tpsk.tj.stokvel.domain.usecase.UploadContributionUseCase
import com.tpsk.tj.stokvel.domain.usecase.VerifyContributionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ContributionViewModel @Inject constructor(
    private val uploadContributionUseCase: UploadContributionUseCase,
    private val verifyContributionUseCase: VerifyContributionUseCase
) : ViewModel() {

    private val _uploadState = MutableStateFlow<Resource<StokvelContribution>?>(null)
    val uploadState: StateFlow<Resource<StokvelContribution>?> = _uploadState

    private val _verifyState = MutableStateFlow<Resource<StokvelContribution>?>(null)
    val verifyState: StateFlow<Resource<StokvelContribution>?> = _verifyState

    fun maskVoucherSerial(serial: String): String {
        if (serial.length < 7) return "xxxx"
        return serial.substring(0, 4) + "x".repeat(serial.length - 7) + serial.substring(serial.length - 3)
    }

    fun uploadContribution(
        groupId: String,
        memberId: String,
        amount: Double,
        method: PaymentMethod,
        voucherSerial: String? = null,
        file: File? = null,
        idempotencyKey: String? = null
    ) {
        viewModelScope.launch {
            uploadContributionUseCase(groupId, memberId, amount, method, voucherSerial, file, idempotencyKey).collect { result ->
                _uploadState.value = result
            }
        }
    }

    fun verifyContribution(
        groupId: String,
        contributionId: String,
        verifierUserId: String,
        action: String,
        reason: String? = null
    ) {
        viewModelScope.launch {
            verifyContributionUseCase(groupId, contributionId, verifierUserId, action, reason).collect { result ->
                _verifyState.value = result
            }
        }
    }
}
