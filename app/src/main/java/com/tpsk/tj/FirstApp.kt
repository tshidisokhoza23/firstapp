package com.tpsk.tj

import android.app.Application
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import com.tpsk.tj.common.data.worker.SyncWorker
import com.tpsk.tj.common.util.ContactSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
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
    @Inject lateinit var adManager: com.tpsk.tj.common.data.ads.AdManager

    override fun onCreate() {
        super.onCreate()
        adManager.initialize(this)
        com.tpsk.tj.common.util.ContextProvider.context = applicationContext
        
        scheduleSync()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        com.tpsk.tj.media.data.ShortsVideoCache.onTrimMemory(level)
    }

    private fun scheduleSync() {
        try {
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

            val contactRequest = PeriodicWorkRequestBuilder<ContactSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "contact_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                contactRequest
            )
        } catch (e: Exception) {
            android.util.Log.e("TJ", "Failed to schedule sync", e)

        }
    }
}
