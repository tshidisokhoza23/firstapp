package com.example.firstapp.media.data.realtime

import android.util.Log
import com.example.firstapp.media.data.local.dao.PostDao
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
        views: Long?
    ) {
        if (postId == null) return
        
        scope.launch {
            try {
                if (likes != null) {
                    postDao.updateLikeCountOnly(postId, likes)
                }
                if (reposts != null) {
                    // Note: Local DAO uses repostCount field
                    postDao.updateRepostCountOnly(postId, reposts)
                }
                if (shares != null) {
                    postDao.updateShareStatus(postId, shares)
                }
                // Views count update if needed
            } catch (e: Exception) {
                Log.e("RealtimeEngagement", "Error updating post $postId engagement", e)
            }
        }
    }

    override fun onShortsEvent(action: String, shortId: String?) {
        // Shorts are also posts, onPostsEvent handles them if they use same ID space
    }

    override fun onStoriesEvent(action: String, storyId: String?) {}

    override fun onError(error: String) {
        Log.e("RealtimeEngagement", "WebSocket error: $error")
    }
}
