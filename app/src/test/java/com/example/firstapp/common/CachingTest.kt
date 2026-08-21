package com.example.firstapp.common

import com.example.firstapp.chat.data.local.dao.ChatDao
import com.example.firstapp.chat.data.repository.ChatRepositoryImpl
import com.example.firstapp.chat.model.Message
import com.example.firstapp.common.util.Resource
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import kotlin.collections.*

@OptIn(ExperimentalCoroutinesApi::class)
class CachingTest {

    private val chatDao: ChatDao = mockk(relaxed = true)
    private val chatApi: com.example.firstapp.chat.data.api.ChatApi = mockk(relaxed = true)
    private val userApi: com.example.firstapp.common.data.api.UserApi = mockk(relaxed = true)
    private val contactDao: com.example.firstapp.chat.data.local.dao.ContactDao = mockk(relaxed = true)
    private val syncCursorDao: com.example.firstapp.chat.data.local.dao.SyncCursorDao = mockk(relaxed = true)
    private val callLogDao: com.example.firstapp.chat.data.local.dao.CallLogDao = mockk(relaxed = true)
    private val displayNameResolver: com.example.firstapp.common.util.DisplayNameResolver = mockk(relaxed = true)
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager = mockk(relaxed = true)
    private val appDatabase: com.example.firstapp.common.data.local.AppDatabase = mockk(relaxed = true)
    private val webSocketManager: com.example.firstapp.common.data.realtime.WebSocketManager = mockk(relaxed = true)
    private val context: android.content.Context = mockk(relaxed = true)

    @Test
    fun `chat cache prunes older messages to maintain LRU state`() = runTest {
        val repository = ChatRepositoryImpl(
            chatApi,
            userApi,
            javax.inject.Provider<ChatDao> { chatDao },
            javax.inject.Provider<com.example.firstapp.chat.data.local.dao.ContactDao> { contactDao },
            javax.inject.Provider<com.example.firstapp.chat.data.local.dao.SyncCursorDao> { syncCursorDao },
            javax.inject.Provider<com.example.firstapp.chat.data.local.dao.CallLogDao> { callLogDao },
            sessionManager,
            javax.inject.Provider<com.example.firstapp.common.data.local.AppDatabase> { appDatabase },
            webSocketManager,
            displayNameResolver,
            context
        )
        val chatId = "chat1"
        val message = Message(id = "msg1", conversationId = chatId, content = "Hello", senderId = "1", senderName = "User", timestamp = 123L)
        
        coEvery { chatDao.insertMessages(any()) } just Runs
        coEvery { chatDao.pruneMessages(chatId, 50) } just Runs
        
        repository.saveMessage(chatId, message)
        
        coVerify { 
            chatDao.insertMessages(any())
            chatDao.pruneMessages(chatId, 50) 
        }
    }
}
