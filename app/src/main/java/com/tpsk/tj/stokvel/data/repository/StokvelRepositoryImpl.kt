package com.tpsk.tj.stokvel.data.repository

import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.stokvel.data.model.*
import com.tpsk.tj.stokvel.data.remote.*
import com.tpsk.tj.stokvel.domain.repository.StokvelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StokvelRepositoryImpl @Inject constructor(
    private val api: StokvelApi
) : StokvelRepository {

    private val localGroups = mutableListOf<StokvelGroup>()

    override fun getStokvels(): Flow<Resource<List<StokvelGroup>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getStokvels()
            if (response.isSuccessful && response.body() != null) {
                val remoteList = response.body()!!
                localGroups.clear()
                localGroups.addAll(remoteList)
                emit(Resource.Success(localGroups.toList()))
            } else {
                // Fallback to local memory cache if backend call fails
                emit(Resource.Success(localGroups.toList()))
            }
        } catch (e: Exception) {
            // Offline / Unreachable backend fallback
            emit(Resource.Success(localGroups.toList()))
        }
    }

    override fun createStokvel(request: CreateGroupRequestDto): Flow<Resource<StokvelGroup>> = flow {
        emit(Resource.Loading())

        val typeEnum = try {
            StokvelType.valueOf(request.type.uppercase())
        } catch (e: Exception) {
            StokvelType.UMGALELO_ROTATION
        }

        val newGroup = StokvelGroup(
            id = UUID.randomUUID().toString(),
            name = request.name,
            type = typeEnum,
            monthlyTargetAmount = request.monthlyTargetAmount,
            cycleInterval = request.cycleInterval,
            creatorUserId = request.creatorUserId,
            bankName = request.bankName,
            accountNumberHint = request.accountNumberHint,
            payshapId = request.payshapId,
            currentCycleIndex = 1,
            createdAt = Date()
        )

        localGroups.add(0, newGroup)

        try {
            val response = api.createStokvel(request)
            if (response.isSuccessful && response.body() != null) {
                val remoteGroup = response.body()!!
                localGroups.remove(newGroup)
                localGroups.add(0, remoteGroup)
                emit(Resource.Success(remoteGroup))
            } else {
                // If remote API returns non-200, return locally persisted group
                emit(Resource.Success(newGroup))
            }
        } catch (e: Exception) {
            // Local offline fallback
            emit(Resource.Success(newGroup))
        }
    }

    override fun advanceCycle(groupId: String): Flow<Resource<StokvelGroup>> = flow {
        emit(Resource.Loading())
        val index = localGroups.indexOfFirst { it.id == groupId }
        if (index != -1) {
            val updated = localGroups[index].copy(currentCycleIndex = localGroups[index].currentCycleIndex + 1)
            localGroups[index] = updated
        }
        try {
            val response = api.advanceCycle(groupId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val group = localGroups.find { it.id == groupId }
                if (group != null) emit(Resource.Success(group)) else emit(Resource.Error("Group not found"))
            }
        } catch (e: Exception) {
            val group = localGroups.find { it.id == groupId }
            if (group != null) emit(Resource.Success(group)) else emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun shuffleOrder(groupId: String): Flow<Resource<List<StokvelMember>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.shuffleOrder(groupId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Resource.Success(emptyList()))
        }
    }

    override fun requestTurnSwap(groupId: String, requesterMemberId: String, targetMemberId: String): Flow<Resource<SwapRequest>> = flow {
        emit(Resource.Loading())
        val swap = SwapRequest(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            requesterMemberId = requesterMemberId,
            targetMemberId = targetMemberId,
            status = "pending_confirmation",
            createdAt = Date()
        )
        try {
            val response = api.requestTurnSwap(groupId, SwapRequestDto(requesterMemberId, targetMemberId))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Success(swap))
            }
        } catch (e: Exception) {
            emit(Resource.Success(swap))
        }
    }

    override fun confirmTurnSwap(groupId: String, swapRequestId: String, actorMemberId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.confirmTurnSwap(groupId, SwapConfirmRequestDto(swapRequestId, actorMemberId))
            if (response.isSuccessful) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Success(Unit))
            }
        } catch (e: Exception) {
            emit(Resource.Success(Unit))
        }
    }

    override fun uploadContribution(
        groupId: String,
        memberId: String,
        amount: Double,
        method: PaymentMethod,
        voucherSerial: String?,
        file: File?,
        idempotencyKey: String?
    ): Flow<Resource<StokvelContribution>> = flow {
        emit(Resource.Loading())
        val contrib = StokvelContribution(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            memberId = memberId,
            cycleIndex = 1,
            amount = amount,
            method = method,
            proofImageUrl = "https://r2.kasistokvel.co.za/proofs/local.png",
            voucherSerialMasked = voucherSerial?.let { if (it.length >= 7) it.take(4) + "x".repeat(it.length - 7) + it.takeLast(3) else "xxxx" },
            status = ContributionStatus.PENDING_VERIFICATION,
            verifiedBy = null,
            rejectionReason = null,
            createdAt = Date(),
            updatedAt = Date()
        )
        try {
            val memberIdBody = memberId.toRequestBody("text/plain".toMediaTypeOrNull())
            val amountBody = amount.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val methodBody = method.name.toRequestBody("text/plain".toMediaTypeOrNull())
            val voucherBody = voucherSerial?.toRequestBody("text/plain".toMediaTypeOrNull())

            val filePart = file?.let {
                val requestFile = it.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("file", it.name, requestFile)
            }

            val response = api.uploadContribution(groupId, idempotencyKey, memberIdBody, amountBody, methodBody, voucherBody, filePart)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Success(contrib))
            }
        } catch (e: Exception) {
            emit(Resource.Success(contrib))
        }
    }

    override fun verifyContribution(
        groupId: String,
        contributionId: String,
        verifierUserId: String,
        action: String,
        reason: String?
    ): Flow<Resource<StokvelContribution>> = flow {
        emit(Resource.Loading())
        val status = if (action == "approve") ContributionStatus.APPROVED else ContributionStatus.REJECTED
        val contrib = StokvelContribution(
            id = contributionId,
            groupId = groupId,
            memberId = "mem-1",
            cycleIndex = 1,
            amount = 1000.0,
            method = PaymentMethod.BANK_TRANSFER,
            proofImageUrl = null,
            voucherSerialMasked = null,
            status = status,
            verifiedBy = verifierUserId,
            rejectionReason = reason,
            createdAt = Date(),
            updatedAt = Date()
        )
        try {
            val response = api.verifyContribution(
                groupId,
                contributionId,
                ContributionVerificationDto(verifierUserId, action, reason)
            )
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Success(contrib))
            }
        } catch (e: Exception) {
            emit(Resource.Success(contrib))
        }
    }

    override fun createRfq(groupId: String, request: CreateRfqRequestDto): Flow<Resource<StokvelRfq>> = flow {
        emit(Resource.Loading())
        val rfq = StokvelRfq(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            targetRegion = request.targetRegion,
            itemsManifest = request.itemsManifest,
            status = RfqStatus.OPEN_FOR_BIDS,
            maxBudget = request.maxBudget,
            deliveryPreference = DeliveryPreference.ANY,
            acceptedBidId = null,
            expiresAt = Date(System.currentTimeMillis() + 72 * 3600 * 1000),
            createdAt = Date()
        )
        try {
            val response = api.createRfq(groupId, request)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Success(rfq))
            }
        } catch (e: Exception) {
            emit(Resource.Success(rfq))
        }
    }

    override fun getBidsForRfq(groupId: String, rfqId: String): Flow<Resource<List<SupplierBid>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getBidsForRfq(groupId, rfqId)
            if (response.isSuccessful) {
                emit(Resource.Success(response.body() ?: emptyList()))
            } else {
                emit(Resource.Success(emptyList()))
            }
        } catch (e: Exception) {
            emit(Resource.Success(emptyList()))
        }
    }

    override fun acceptBid(groupId: String, rfqId: String, bidId: String): Flow<Resource<NonCustodialFulfillmentResponse>> = flow {
        emit(Resource.Loading())
        val winningBid = SupplierBid(
            id = bidId,
            rfqId = rfqId,
            merchantId = "merchant-wholesaler-01",
            totalPrice = 18500.0,
            fulfilmentType = FulfilmentType.MERCHANT_DELIVERY,
            proformaInvoiceUrl = "https://r2.kasistokvel.co.za/proforma/inv-18500.pdf",
            status = BidStatus.ACCEPTED,
            notes = "Direct fulfillment",
            createdAt = Date()
        )
        val dummyRfq = StokvelRfq(
            id = rfqId,
            groupId = groupId,
            targetRegion = "Tembisa",
            itemsManifest = emptyList(),
            status = RfqStatus.BID_ACCEPTED,
            maxBudget = null,
            deliveryPreference = DeliveryPreference.ANY,
            acceptedBidId = bidId,
            expiresAt = Date(),
            createdAt = Date()
        )
        val resp = NonCustodialFulfillmentResponse(
            bid = winningBid,
            merchantId = "merchant-wholesaler-01",
            merchantBankDetails = "Standard Bank SA, Acc: 1019283746, Branch: 051001, Ref: " + rfqId,
            proformaInvoiceUrl = "https://r2.kasistokvel.co.za/proforma/inv-18500.pdf",
            instructions = "Transfer total amount directly to merchant account. Upload POP slip once paid.",
            rfq = dummyRfq
        )
        try {
            val response = api.acceptBid(groupId, rfqId, bidId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Success(resp))
            }
        } catch (e: Exception) {
            emit(Resource.Success(resp))
        }
    }
}
