package com.tpsk.tj.stokvel.data.model

import java.util.Date

enum class RfqStatus {
    DRAFT,
    OPEN_FOR_BIDS,
    BID_ACCEPTED,
    FULFILLED,
    CANCELLED
}

enum class DeliveryPreference {
    PICKUP_ONLY,
    DELIVERY_REQUIRED,
    ANY
}

data class RfqItem(
    val name: String,
    val quantity: Int,
    val unit: String
)

data class StokvelRfq(
    val id: String,
    val groupId: String,
    val targetRegion: String,
    val itemsManifest: List<RfqItem>,
    val status: RfqStatus,
    val maxBudget: Double?,
    val deliveryPreference: DeliveryPreference,
    val acceptedBidId: String?,
    val expiresAt: Date,
    val createdAt: Date
)
