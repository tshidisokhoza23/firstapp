package com.tpsk.tj.stokvel.data.model

import java.util.Date

enum class PaymentMethod {
    BANK_TRANSFER,
    PAYSHAP,
    ONE_VOUCHER,
    CASH
}

enum class ContributionStatus {
    PENDING_VERIFICATION,
    APPROVED,
    REJECTED
}

data class StokvelContribution(
    val id: String,
    val groupId: String,
    val memberId: String,
    val cycleIndex: Int,
    val amount: Double,
    val method: PaymentMethod,
    val proofImageUrl: String?, // Encrypted URL on Cloudflare R2
    val voucherSerialMasked: String?, // Optional, for 1Voucher audit
    val status: ContributionStatus,
    val verifiedBy: String?, // UUID of the admin/treasurer
    val rejectionReason: String?,
    val createdAt: Date,
    val updatedAt: Date
)
