package com.example.firstapp.common.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.firstapp.common.data.api.SyncAccountContactsRequest
import com.example.firstapp.common.data.api.UserApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import androidx.hilt.work.HiltWorker

@HiltWorker
class ContactSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: com.example.firstapp.domain.repository.ChatRepository,
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!sessionManager.isLoggedIn()) {
            return Result.success()
        }

        Log.d("ContactSyncWorker", "Starting background contact sync")
        val fetcher = ContactsFetcher(applicationContext)
        val contactList = fetcher.fetchAll()
        
        // Use the repository's syncContacts which now handles persistent storage + local DB updates
        return try {
            repository.syncContacts(contactList.map { it.phone_number }).collect { }
            Log.d("ContactSyncWorker", "Contact sync successful")
            Result.success()
        } catch (e: Exception) {
            Log.e("ContactSyncWorker", "Contact sync error", e)
            Result.retry()
        }
    }
}
