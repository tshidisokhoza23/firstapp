package com.example.firstapp.chat.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.firstapp.chat.data.api.ChatApi
import com.example.firstapp.chat.data.local.entity.MessageEntity
import com.example.firstapp.chat.data.repository.mapApiMessage
import com.example.firstapp.chat.data.repository.toEntity
import com.example.firstapp.chat.model.Message
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.common.domain.auth.SessionManager
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val conversationId: String,
    private val api: ChatApi,
    private val db: AppDatabase,
    private val sessionManager: SessionManager
) : RemoteMediator<Int, MessageEntity>() {

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.SKIP_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageEntity>
    ): MediatorResult {
        try {
            val cursor: String? = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                    if (lastItem == null) {
                        return MediatorResult.Success(endOfPaginationReached = false)
                    }
                    lastItem.id
                }
            }

            Log.d("MessageRemoteMediator", "Loading $loadType for $conversationId, cursor=$cursor")
            
            val response = api.getMessages(
                conversationID = conversationId,
                cursor = cursor,
                limit = state.config.pageSize
            )

            if (!response.isSuccessful) {
                return MediatorResult.Error(HttpException(response))
            }

            val apiResponse = response.body() ?: return MediatorResult.Error(IOException("Empty body"))
            val apiMessages = apiResponse.messages
            val endOfPaginationReached = apiMessages.isEmpty()

            val myUserId = sessionManager.getUserId()
            val privKey = sessionManager.getPrivateKey()

            db.withTransaction {
                val entities = apiMessages.map { apiMsg ->
                    val message: Message = mapApiMessage(apiMsg, privKey, myUserId)
                    message.toEntity(conversationId)
                }
                db.chatDao().insertMessages(entities)
            }

            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            return MediatorResult.Error(e)
        } catch (e: HttpException) {
            return MediatorResult.Error(e)
        } catch (e: Exception) {
            Log.e("MessageRemoteMediator", "Unexpected error", e)
            return MediatorResult.Error(e)
        }
    }
}
