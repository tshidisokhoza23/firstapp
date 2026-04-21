package com.example.firstapp.data.media

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

@UnstableApi
object ShortsVideoCache {
    private const val CACHE_DIR = "shorts_video_cache"
    private const val MAX_CACHE_SIZE_BYTES = 180L * 1024L * 1024L
    private const val MAX_PRELOAD_PLAYERS = 3

    @Volatile
    private var cache: SimpleCache? = null

    @Volatile
    private var databaseProvider: DatabaseProvider? = null

    private val preloadPlayers = LinkedHashMap<String, ExoPlayer>(MAX_PRELOAD_PLAYERS, 0.75f, true)

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
    fun buildPlayer(context: Context): ExoPlayer {
        return ExoPlayer.Builder(appContext(context))
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory(context)))
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }

    @Synchronized
    fun preload(context: Context, urls: List<String>) {
        val wantedUrls = urls
            .asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_PRELOAD_PLAYERS)
            .toList()

        val staleUrls = preloadPlayers.keys.filterNot { it in wantedUrls }
        staleUrls.forEach { url ->
            preloadPlayers.remove(url)?.release()
        }

        wantedUrls.forEach { url ->
            if (preloadPlayers.containsKey(url)) return@forEach

            val player = buildPlayer(context).apply {
                volume = 0f
                playWhenReady = false
                setMediaItem(MediaItem.fromUri(url))
                prepare()
            }
            preloadPlayers[url] = player
        }

        while (preloadPlayers.size > MAX_PRELOAD_PLAYERS) {
            val eldestUrl = preloadPlayers.entries.firstOrNull()?.key ?: break
            preloadPlayers.remove(eldestUrl)?.release()
        }
    }

    @Synchronized
    fun releasePreloadsExcept(urls: List<String>) {
        val retained = urls.filter { it.isNotBlank() }.toSet()
        val staleUrls = preloadPlayers.keys.filterNot { it in retained }
        staleUrls.forEach { url ->
            preloadPlayers.remove(url)?.release()
        }
    }
}