package com.example.firstapp.media.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

import kotlinx.coroutines.Job

@UnstableApi
object ShortsVideoCache {
    private val preloadJobs = mutableMapOf<String, Job>()
    private var currentPlayer: ExoPlayer? = null
    private var currentUrl: String? = null

    private fun appContext(context: Context): Context = context.applicationContext

    private fun dataSourceFactory(context: Context): DefaultDataSource.Factory {
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        return DefaultDataSource.Factory(appContext(context), upstreamFactory)
    }

    @Synchronized
    fun acquirePlayer(context: Context, url: String): ExoPlayer {
        // Stop any previous preloads for this specific URL if they are still running
        preloadJobs[url]?.cancel()
        preloadJobs.remove(url)

        val player = currentPlayer ?: ExoPlayer.Builder(appContext(context))
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory(context)))
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
        
        currentPlayer = player
        currentUrl = url
        return player
    }

    @Synchronized
    fun preload(context: Context, urls: List<String>) {
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
    }

    @Synchronized
    fun onTrimMemory(level: Int) {
        // Cancel all preloads to free up network and I/O
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
        
        if (level >= 80) { // ComponentCallbacks2.TRIM_MEMORY_COMPLETE
             forceRelease()
        } else if (level >= 20) { // ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
             currentPlayer?.stop()
        }
    }

    @Synchronized
    fun releasePlayer(player: ExoPlayer, url: String) {
        if (player === currentPlayer) {
            if (url == currentUrl) {
                // Only stop if this specific URL is being released and no one else claimed it
                player.stop()
                player.clearMediaItems()
                currentUrl = null
            }
            // Otherwise, keep it alive for the next caller who already updated currentUrl
        } else {
            player.release()
        }
    }
    
    @Synchronized
    fun forceRelease() {
        currentPlayer?.release()
        currentPlayer = null
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
    }
}
