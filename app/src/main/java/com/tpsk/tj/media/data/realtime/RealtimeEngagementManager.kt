package com.tpsk.tj.media.data.realtime

import android.util.Log
import com.tpsk.tj.media.data.local.dao.PostDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeEngagementManager @Inject constructor(
    private val webSocketClient: PostWebSocketClient,
    private val postDao: PostDao
) : PostSocketListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        webSocketClient.connect(this)
    }

    fun stop() {
        webSocketClient.disconnect(this)
    }

    override fun onOpen() {
        Log.d("RealtimeEngagement", "WebSocket connected, listening for engagement events")
    }

    override fun onPostsEvent(
        action: String,
        postId: String?,
        likes: Long?,
        reposts: Long?,
        shares: Long?,
        views: Long?,
        replies: Long?
    ) {
        applyCounts(postId, likes, reposts, shares, views, replies)
    }

    override fun onShortsEvent(
        action: String,
        shortId: String?,
        likes: Long?,
        reposts: Long?,
        shares: Long?,
        views: Long?,
        replies: Long?
    ) {
        applyCounts(shortId, likes, reposts, shares, views, replies)
    }

    private fun applyCounts(
        postId: String?,
        likes: Long?,
        reposts: Long?,
        shares: Long?,
        views: Long?,
        replies: Long?
    ) {
        if (postId.isNullOrBlank()) return

        scope.launch {
            try {
                if (likes != null) {
                    postDao.updateLikeCountOnly(postId, likes)
                }
                if (reposts != null) {
                    postDao.updateRepostCountOnly(postId, reposts)
                }
                if (shares != null) {
                    postDao.updateShareStatus(postId, shares)
                }
                if (views != null) {
                    postDao.updateViewCount(postId, views)
                }
                if (replies != null) {
                    postDao.updateRepliesCount(postId, replies)
                }
            } catch (e: Exception) {
                Log.e("RealtimeEngagement", "Error updating post $postId engagement", e)
            }
        }
    }

    override fun onStoriesEvent(action: String, storyId: String?) {}

    override fun onError(error: String) {
        Log.e("RealtimeEngagement", "WebSocket error: $error")
    }
}
