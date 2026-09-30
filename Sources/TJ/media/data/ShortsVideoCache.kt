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

    // Multi-player pool mapped by URL
    private val playerPool = ConcurrentHashMap<String, ExoPlayer>()
    private val poolUrls = java.util.Collections.synchronizedList(mutableListOf<String>())
    private const val MAX_POOL_SIZE = 4

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
        val existing = playerPool[url]
        if (existing != null) {
            poolUrls.remove(url)
            poolUrls.add(url)
            return existing
        }

        // If pool is full, evict oldest player
        if (playerPool.size >= MAX_POOL_SIZE && poolUrls.isNotEmpty()) {
            val oldestUrl = poolUrls.removeAt(0)
            val oldestPlayer = playerPool.remove(oldestUrl)
            oldestPlayer?.stop()
            oldestPlayer?.release()
        }

        val newPlayer = ExoPlayer.Builder(appContext(context))
            .setMediaSourceFactory(DefaultMediaSourceFactory(createCacheDataSourceFactory(context)))
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(url))
                prepare()
            }

        playerPool[url] = newPlayer
        poolUrls.add(url)
        return newPlayer
    }

    @Synchronized
    fun releasePlayer(url: String) {
        val player = playerPool[url]
        player?.playWhenReady = false
        // Retain player in pool up to MAX_POOL_SIZE so returning to the short (e.g. from Add Short)
        // reuses the prepared player seamlessly without reloading from 0s.
        if (playerPool.size > MAX_POOL_SIZE && poolUrls.isNotEmpty()) {
            val oldestUrl = poolUrls.removeAt(0)
            val oldestPlayer = playerPool.remove(oldestUrl)
            oldestPlayer?.stop()
            oldestPlayer?.release()
        }
    }

    @Synchronized
    fun releasePlayer(player: ExoPlayer, url: String) {
        val existing = playerPool[url]
        if (existing != null) {
            existing.playWhenReady = false
        } else {
            player.stop()
            player.release()
        }
        if (playerPool.size > MAX_POOL_SIZE && poolUrls.isNotEmpty()) {
            val oldestUrl = poolUrls.removeAt(0)
            val oldestPlayer = playerPool.remove(oldestUrl)
            oldestPlayer?.stop()
            oldestPlayer?.release()
        }
    }

    @Synchronized
    fun pauseAll() {
        playerPool.values.forEach { player ->
            runCatching {
                player.playWhenReady = false
                player.pause()
            }
        }
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
    fun onTrimMemory(level: Int) {
        if (level >= 80) { // ComponentCallbacks2.TRIM_MEMORY_COMPLETE
            forceRelease()
        } else if (level >= 20) { // ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
            playerPool.values.forEach { it.playWhenReady = false }
        }
    }

    @Synchronized
    fun forceRelease() {
        playerPool.values.forEach {
            it.stop()
            it.release()
        }
        playerPool.clear()
        poolUrls.clear()
    }
}
