package com.example.firstapp.common.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.firstapp.domain.repository.ChatRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ChatOutboxWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val chatRepository: ChatRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.d("ChatOutboxWorker", "Checking for pending messages to retry...")
        return try {
            chatRepository.syncPendingMessages()
            Result.success()
        } catch (e: Exception) {
            Log.e("ChatOutboxWorker", "Outbox sync failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "chat_outbox_retry"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<ChatOutboxWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            // KEEP, not REPLACE: syncPendingMessages() drains ALL pending messages each
            // run, so a message sent while a sync is already in flight will still be
            // picked up. REPLACE was cancelling the in-flight sync on every new send,
            // so bursts of messages kept restarting the loop and never confirmed earlier
            // ones (they'd appear stuck "sending" indefinitely).
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
