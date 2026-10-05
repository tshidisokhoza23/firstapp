package com.tpsk.tj.common.data.ads

import android.content.Context

/**
 * Holds a few native ads before a surface needs them.
 * Callers take an ad on the first frame or skip the slot. They never insert
 * a zero-height placeholder and grow it later.
 */
object NativeAdPool {
    private const val TARGET = 3
    private val ready = HashMap<String, ArrayDeque<Any>>()
    private val inflight = HashMap<String, Int>()

    fun warm(
        adManager: AdManager,
        context: Context,
        unitId: String,
        portrait: Boolean,
        target: Int = TARGET
    ) {
        val app = context.applicationContext
        val slot = key(unitId, portrait)
        val need = synchronized(this) {
            val have = (ready[slot]?.size ?: 0) + (inflight[slot] ?: 0)
            val missing = (target - have).coerceAtLeast(0)
            if (missing > 0) inflight[slot] = (inflight[slot] ?: 0) + missing
            missing
        }
        repeat(need) {
            val onLoaded: (Any) -> Unit = { ad -> store(slot, ad) }
            val onFailed: (String) -> Unit = { releaseInflight(slot) }
            if (portrait) adManager.loadShortsNativeAd(app, unitId, onLoaded, onFailed)
            else adManager.loadNativeAd(app, unitId, onLoaded, onFailed)
        }
    }

    fun take(
        adManager: AdManager,
        context: Context,
        unitId: String,
        portrait: Boolean
    ): Any? {
        val slot = key(unitId, portrait)
        val ad = synchronized(this) { ready[slot]?.removeFirstOrNull() }
        warm(adManager, context, unitId, portrait)
        return ad
    }

    fun destroyAll(adManager: AdManager) {
        val ads = synchronized(this) {
            val all = ready.values.flatMap { it.toList() }
            ready.clear()
            inflight.clear()
            all
        }
        ads.forEach { adManager.destroyAd(it) }
    }

    private fun store(slot: String, ad: Any) {
        synchronized(this) {
            inflight[slot] = ((inflight[slot] ?: 1) - 1).coerceAtLeast(0)
            ready.getOrPut(slot) { ArrayDeque() }.addLast(ad)
        }
    }

    private fun releaseInflight(slot: String) {
        synchronized(this) {
            inflight[slot] = ((inflight[slot] ?: 1) - 1).coerceAtLeast(0)
        }
    }

    private fun key(unitId: String, portrait: Boolean): String =
        "$unitId|${if (portrait) "portrait" else "feed"}"
}

/**
 * Feed slots that were empty on their first frame stay empty.
 * Slots that already reserved 320dp stay 320dp, even after the ad is recycled.
 */
object FeedAdSlots {
    private val reserved = HashSet<String>()

    fun reserveIfReady(slotKey: String, readyNow: Boolean): Boolean {
        if (slotKey in reserved) return true
        if (!readyNow) return false
        reserved.add(slotKey)
        return true
    }
}
