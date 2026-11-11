package com.tpsk.tj.stokvel.domain.usecase

import com.tpsk.tj.stokvel.data.model.PaymentMethod
import com.tpsk.tj.stokvel.data.remote.CreateGroupRequestDto
import com.tpsk.tj.stokvel.data.remote.CreateRfqRequestDto
import com.tpsk.tj.stokvel.domain.repository.StokvelRepository
import java.io.File
import javax.inject.Inject

class GetStokvelsUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke() = repository.getStokvels()
}

class CreateStokvelUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke(request: CreateGroupRequestDto) = repository.createStokvel(request)
}

class AdvanceCycleUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke(groupId: String) = repository.advanceCycle(groupId)
}

class ShuffleOrderUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke(groupId: String) = repository.shuffleOrder(groupId)
}

class RequestTurnSwapUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke(groupId: String, requesterMemberId: String, targetMemberId: String) =
        repository.requestTurnSwap(groupId, requesterMemberId, targetMemberId)
}

class ConfirmTurnSwapUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke(groupId: String, swapRequestId: String, actorMemberId: String) =
        repository.confirmTurnSwap(groupId, swapRequestId, actorMemberId)
}

class UploadContributionUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke(
        groupId: String,
        memberId: String,
        amount: Double,
        method: PaymentMethod,
        voucherSerial: String? = null,
        file: File? = null,
        idempotencyKey: String? = null
    ) = repository.uploadContribution(groupId, memberId, amount, method, voucherSerial, file, idempotencyKey)
}

class VerifyContributionUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke(
        groupId: String,
        contributionId: String,
        verifierUserId: String,
        action: String,
        reason: String? = null
    ) = repository.verifyContribution(groupId, contributionId, verifierUserId, action, reason)
}

class CreateRfqUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke(groupId: String, request: CreateRfqRequestDto) =
        repository.createRfq(groupId, request)
}

class GetBidsForRfqUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke(groupId: String, rfqId: String) = repository.getBidsForRfq(groupId, rfqId)
}

class AcceptBidUseCase @Inject constructor(private val repository: StokvelRepository) {
    operator fun invoke(groupId: String, rfqId: String, bidId: String) =
        repository.acceptBid(groupId, rfqId, bidId)
}
