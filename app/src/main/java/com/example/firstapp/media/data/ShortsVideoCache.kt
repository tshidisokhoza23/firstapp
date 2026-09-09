package com.example.firstapp.media.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File

@UnstableApi
object ShortsVideoCache {
    @Volatile
    private var simpleCache: SimpleCache? = null
    private var currentPlayer: ExoPlayer? = null
    private var currentUrl: String? = null

    private fun appContext(context: Context): Context = context.applicationContext

    private fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(appContext(context).cacheDir, "shorts_video_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024L)
                val databaseProvider = StandaloneDatabaseProvider(appContext(context))
                SimpleCache(cacheDir, evictor, databaseProvider).also { simpleCache = it }
            }
        }
    }

    private fun createCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        val upstreamFactory = DefaultDataSource.Factory(appContext(context), httpFactory)

        return CacheDataSource.Factory()
            .setCache(getCache(context))
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Synchronized
    fun acquirePlayer(context: Context, url: String): ExoPlayer {
        val player = currentPlayer ?: ExoPlayer.Builder(appContext(context))
            .setMediaSourceFactory(DefaultMediaSourceFactory(createCacheDataSourceFactory(context)))
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
        currentPlayer = player
        
        if (currentUrl != url) {
            currentUrl = url
            player.stop()
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
        }
        return player
    }

    @Synchronized
    fun preload(context: Context, urls: List<String>) {
        // SimpleCache automatically retains cached segments on disk
    }

    @Synchronized
    fun releasePlayer(player: ExoPlayer, url: String) {
        if (player === currentPlayer) {
            if (url == currentUrl) {
                player.stop()
                currentUrl = null
            }
        } else {
            player.release()
        }
    }

    @Synchronized
    fun onTrimMemory(level: Int) {
        if (level >= 80) { // ComponentCallbacks2.TRIM_MEMORY_COMPLETE
            forceRelease()
        } else if (level >= 20) { // ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
            currentPlayer?.stop()
        }
    }

    @Synchronized
    fun forceRelease() {
        currentPlayer?.stop()
        currentPlayer?.release()
        currentPlayer = null
        currentUrl = null
    }
}
