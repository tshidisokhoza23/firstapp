package com.tpsk.tj.ui.members

enum class MembershipTier(
    val tierLevel: Int,
    val label: String,
    val priceDisplay: String,
    val sku: String?,
    val priceCents: Long,
    val memberCap: Int?
) {
    FREE(0, "Free", "R0", null, 0L, null),
    TIER_1(1, "Tier 1: Community", "R50 / mo", "member_sub_50", 5_000L, null),
    TIER_2(2, "Tier 2: Insider", "R90 / mo", "member_sub_90", 9_000L, null),
    TIER_3(3, "Tier 3: Analysis", "R170 / mo", "member_sub_170", 17_000L, null),
    TIER_4(4, "Tier 4: Mastermind", "R350 / mo", "member_sub_350", 35_000L, null),
    TIER_5(5, "Tier 5: Executive", "R750 / mo", "member_sub_750", 75_000L, 500),
    TIER_6(6, "Tier 6: Pro Syndicate", "R1,500 / mo", "member_sub_1500", 150_000L, 250),
    TIER_7(7, "Tier 7: Elite Inner Circle", "R3,500 / mo", "member_sub_3500", 350_000L, 100),
    TIER_8(8, "Tier 8: Private Board", "R8,000 / mo", "member_sub_8000", 800_000L, 50),
    TIER_9(9, "Tier 9: Sovereign Advisory", "R18,000 / mo", "member_sub_18000", 1_800_000L, 25)
}
