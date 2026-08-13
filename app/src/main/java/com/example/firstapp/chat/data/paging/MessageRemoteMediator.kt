package com.example.firstapp.chat.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.firstapp.chat.data.api.ChatApi
import com.example.firstapp.chat.data.local.entity.MessageEntity
import com.example.firstapp.chat.data.repository.mapApiMessage
import com.example.firstapp.chat.data.repository.toEntity
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.common.domain.auth.SessionManager
import retrofit2.HttpException
import java.io.IOException

/**
 * MessageRemoteMediator
 * Implements Paging 3 RemoteMediator for offline-first chat history.
 * Synchronizes network messages into Room as the Single Source of Truth.
 */
@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val conversationId: String,
    private val api: ChatApi,
    private val db: AppDatabase,
    private val sessionManager: SessionManager
) : RemoteMediator<Int, MessageEntity>() {

    override suspend fun initialize(): InitializeAction {
        // Launch a refresh on first load to ensure we have the latest messages
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageEntity>
    ): MediatorResult {
        return try {
            val cursor: String? = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    // Get the oldest message in the current list to use as a cursor for older messages
                    val lastItem = state.lastItemOrNull()
                    if (lastItem == null) {
                        return MediatorResult.Success(endOfPaginationReached = false)
                    }
                    lastItem.id
                }
            }

            // Fetch from network
            val response = api.getMessages(
                conversationID = conversationId,
                cursor = cursor,
                limit = state.config.pageSize
            )

            if (!response.isSuccessful) {
                return MediatorResult.Error(HttpException(response))
            }

            val apiMessages = response.body()?.messages ?: emptyList()
            val endOfPaginationReached = apiMessages.isEmpty()

            val myUserId = sessionManager.getUserId()
            val privKey = sessionManager.getPrivateKey()

            db.withTransaction {
                // If this is a refresh, we've fetched the absolute latest.
                // We keep existing messages to support offline history.
                
                val entities = apiMessages.map { 
                    mapApiMessage(it, privKey, myUserId).toEntity(conversationId)
                }
                
                // Insert into SSOT
                db.chatDao().insertMessages(entities)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}
