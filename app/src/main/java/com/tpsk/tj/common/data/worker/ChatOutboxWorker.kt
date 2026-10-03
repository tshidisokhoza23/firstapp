package com.tpsk.tj.common.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tpsk.tj.domain.repository.ChatRepository
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

            // APPEND, not REPLACE: REPLACE cancelled the in-flight sync on every
            // new send, so bursts never confirmed the earlier message. KEEP also
            // dropped a media send that was written after the running worker had
            // already read the pending list.
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND,
                request
            )
        }
    }
}
