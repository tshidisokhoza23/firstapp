package com.tpsk.tj.stokvel.data.model

data class NonCustodialFulfillmentResponse(
    val bid: SupplierBid,
    val merchantId: String,
    val merchantBankDetails: String,
    val proformaInvoiceUrl: String,
    val instructions: String,
    val rfq: StokvelRfq
)
