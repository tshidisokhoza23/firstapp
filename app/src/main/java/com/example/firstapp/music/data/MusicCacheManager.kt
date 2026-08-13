package com.example.firstapp.music.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cache: SimpleCache? = null

    @OptIn(UnstableApi::class)
    @Synchronized
    fun getCache(): SimpleCache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "music_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val evictor = LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024)
            val databaseProvider = StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cache!!
    }

    @OptIn(UnstableApi::class)
    fun release() {
        cache?.release()
        cache = null
    }
}
