package com.example.firstapp.media.data

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import java.io.File
import java.util.LinkedHashMap

import androidx.core.net.toUri
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.DataSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@UnstableApi
object ShortsVideoCache {
    private const val CACHE_DIR = "shorts_video_cache"
    private const val MAX_CACHE_SIZE_BYTES = 180L * 1024L * 1024L
    private const val PRELOAD_BYTES = 1024L * 1024L // Preload 1MB of each next video

    @Volatile
    private var cache: SimpleCache? = null

    @Volatile
    private var databaseProvider: DatabaseProvider? = null

    private val preloadJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var currentPlayer: ExoPlayer? = null

    private fun appContext(context: Context): Context = context.applicationContext

    @Synchronized
    private fun databaseProvider(context: Context): DatabaseProvider {
        return databaseProvider ?: StandaloneDatabaseProvider(appContext(context)).also {
            databaseProvider = it
        }
    }

    @Synchronized
    private fun cache(context: Context): SimpleCache {
        return cache ?: SimpleCache(
            File(appContext(context).cacheDir, CACHE_DIR),
            LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES),
            databaseProvider(context)
        ).also {
            cache = it
        }
    }

    private fun cacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8_000)
            .setReadTimeoutMs(12_000)

        return CacheDataSource.Factory()
            .setCache(cache(context))
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(appContext(context), upstreamFactory)
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    @Synchronized
    fun acquirePlayer(context: Context, url: String): ExoPlayer {
        // Stop any previous preloads for this specific URL if they are still running
        preloadJobs[url]?.cancel()
        preloadJobs.remove(url)

        val player = currentPlayer ?: ExoPlayer.Builder(appContext(context))
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory(context)))
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
        
        currentPlayer = player
        return player
    }

    @Synchronized
    fun preload(context: Context, urls: List<String>) {
        val cache = cache(context)
        val dataSourceFactory = cacheDataSourceFactory(context)

        // Cancel jobs for URLs no longer in the preload list
        val iterator = preloadJobs.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in urls) {
                entry.value.cancel()
                iterator.remove()
            }
        }

        urls.forEach { url ->
            if (url.isBlank() || preloadJobs.containsKey(url)) return@forEach
            
            val job = scope.launch {
                try {
                    val dataSpec = DataSpec(url.toUri(), 0, PRELOAD_BYTES)
                    val writer = CacheWriter(
                        dataSourceFactory.createDataSource(),
                        dataSpec,
                        null,
                        null
                    )
                    writer.cache()
                } catch (_: Exception) {
                    // Preload failed, usually fine as it will load normally when played
                }
            }
            preloadJobs[url] = job
        }
    }

    @Synchronized
    fun onLowMemory() {
        // Cancel all preloads to free up network and I/O
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
        
        // Stop current player but don't release it unless absolutely necessary
        currentPlayer?.stop()
    }

    @Synchronized
    fun releasePlayer(player: ExoPlayer) {
        if (player === currentPlayer) {
            player.stop()
            player.clearMediaItems()
            // We keep the instance alive for reuse to avoid GC pressure/allocation overhead on 2GB RAM
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
