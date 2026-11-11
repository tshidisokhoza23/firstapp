package com.tpsk.tj.stokvel.domain.repository

import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.stokvel.data.model.*
import com.tpsk.tj.stokvel.data.remote.CreateGroupRequestDto
import com.tpsk.tj.stokvel.data.remote.CreateRfqRequestDto
import kotlinx.coroutines.flow.Flow
import java.io.File

interface StokvelRepository {
    fun getStokvels(): Flow<Resource<List<StokvelGroup>>>
    fun createStokvel(request: CreateGroupRequestDto): Flow<Resource<StokvelGroup>>
    fun advanceCycle(groupId: String): Flow<Resource<StokvelGroup>>
    fun shuffleOrder(groupId: String): Flow<Resource<List<StokvelMember>>>
    fun requestTurnSwap(groupId: String, requesterMemberId: String, targetMemberId: String): Flow<Resource<SwapRequest>>
    fun confirmTurnSwap(groupId: String, swapRequestId: String, actorMemberId: String): Flow<Resource<Unit>>
    fun uploadContribution(
        groupId: String,
        memberId: String,
        amount: Double,
        method: PaymentMethod,
        voucherSerial: String? = null,
        file: File? = null,
        idempotencyKey: String? = null
    ): Flow<Resource<StokvelContribution>>
    fun verifyContribution(
        groupId: String,
        contributionId: String,
        verifierUserId: String,
        action: String,
        reason: String? = null
    ): Flow<Resource<StokvelContribution>>
    fun createRfq(groupId: String, request: CreateRfqRequestDto): Flow<Resource<StokvelRfq>>
    fun getBidsForRfq(groupId: String, rfqId: String): Flow<Resource<List<SupplierBid>>>
    fun acceptBid(groupId: String, rfqId: String, bidId: String): Flow<Resource<NonCustodialFulfillmentResponse>>
}
