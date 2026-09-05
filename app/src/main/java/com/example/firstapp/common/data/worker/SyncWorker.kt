package com.example.firstapp.common.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker

import androidx.hilt.work.HiltWorker
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.chat.domain.repository.StoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val chatRepository: ChatRepository,
    private val storyRepository: StoryRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): ListenableWorker.Result {
        return try {
            chatRepository.syncPendingMessages()
            chatRepository.syncDelta().collect {}
            chatRepository.syncChats().collect {}
            if (!storyRepository.syncStories()) {
                return ListenableWorker.Result.retry()
            }
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            ListenableWorker.Result.retry()
        }
    }
}
