package com.tpsk.tj.media.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@UnstableApi
object ShortsVideoCache {
    @Volatile
    private var simpleCache: SimpleCache? = null
    private var currentPlayer: ExoPlayer? = null
    private var currentUrl: String? = null

    private val preloadingJobs = ConcurrentHashMap<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun appContext(context: Context): Context = context.applicationContext

    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(appContext(context).cacheDir, "shorts_video_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024L) // 200MB cache
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

    fun preload(context: Context, urls: List<String>) {
        val cache = getCache(context)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
        
        val upstreamFactory = DefaultDataSource.Factory(appContext(context), httpFactory)

        urls.take(3).forEach { url ->
            if (url.isNotBlank() && !preloadingJobs.containsKey(url)) {
                val job = scope.launch {
                    try {
                        val uri = Uri.parse(url)
                        val dataSpec = DataSpec.Builder()
                            .setUri(uri)
                            .setLength(2 * 1024 * 1024L) // Pre-cache first 2MB for instant playback
                            .build()
                        val cacheDataSource = CacheDataSource(cache, upstreamFactory.createDataSource())
                        val cacheWriter = CacheWriter(
                            cacheDataSource,
                            dataSpec,
                            null,
                            null
                        )
                        cacheWriter.cache()
                    } catch (_: Exception) {
                    } finally {
                        preloadingJobs.remove(url)
                    }
                }
                preloadingJobs[url] = job
            }
        }
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
