package com.tpsk.tj.stokvel.data.model

import java.util.Date

enum class BidStatus {
    SUBMITTED,
    ACCEPTED,
    REJECTED,
    EXPIRED
}

enum class FulfilmentType {
    PICKUP,
    MERCHANT_DELIVERY
}

data class SupplierBid(
    val id: String,
    val rfqId: String,
    val merchantId: String,
    val totalPrice: Double,
    val fulfilmentType: FulfilmentType,
    val proformaInvoiceUrl: String?,
    val status: BidStatus,
    val notes: String?,
    val createdAt: Date
)
