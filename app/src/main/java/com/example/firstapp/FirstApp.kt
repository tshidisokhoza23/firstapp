package com.example.firstapp

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.common.data.worker.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import javax.inject.Inject

@HiltAndroidApp
class FirstApp : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    override fun onCreate() {
        super.onCreate()
        com.example.firstapp.common.util.ContextProvider.context = applicationContext
        TokenManager.initialize(this)
        scheduleSync()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            com.example.firstapp.media.data.ShortsVideoCache.onLowMemory()
        }
        if (level >= TRIM_MEMORY_MODERATE) {
            com.example.firstapp.media.data.ShortsVideoCache.forceRelease()
        }
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "message_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
