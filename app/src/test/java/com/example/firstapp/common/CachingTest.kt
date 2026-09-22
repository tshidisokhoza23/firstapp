package com.tpsk.tj.common

import com.tpsk.tj.chat.data.local.dao.ChatDao
import com.tpsk.tj.chat.data.repository.ChatRepositoryImpl
import com.tpsk.tj.chat.model.Message
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CachingTest {

    private val chatDao: ChatDao = mockk(relaxed = true)
    private val channelDao: com.tpsk.tj.chat.data.local.dao.ChannelDao = mockk(relaxed = true)
    private val chatApi: com.tpsk.tj.chat.data.api.ChatApi = mockk(relaxed = true)
    private val userApi: com.tpsk.tj.common.data.api.UserApi = mockk(relaxed = true)
    private val socialApi: com.tpsk.tj.media.data.api.SocialApi = mockk(relaxed = true)
    private val contactDao: com.tpsk.tj.chat.data.local.dao.ContactDao = mockk(relaxed = true)
    private val syncCursorDao: com.tpsk.tj.chat.data.local.dao.SyncCursorDao = mockk(relaxed = true)
    private val callLogDao: com.tpsk.tj.chat.data.local.dao.CallLogDao = mockk(relaxed = true)
    private val displayNameResolver: com.tpsk.tj.common.util.DisplayNameResolver = mockk(relaxed = true)
    private val sessionManager: com.tpsk.tj.common.domain.auth.SessionManager = mockk(relaxed = true)
    private val appDatabase: com.tpsk.tj.common.data.local.AppDatabase = mockk(relaxed = true)
    private val webSocketManager: com.tpsk.tj.common.data.realtime.WebSocketManager = mockk(relaxed = true)
    private val context: android.content.Context = mockk(relaxed = true)

    @Test
    fun `chat cache saves message into DAO`() = runTest {
        val repository = ChatRepositoryImpl(
            chatApi,
            userApi,
            socialApi,
            javax.inject.Provider<ChatDao> { chatDao },
            javax.inject.Provider<com.tpsk.tj.chat.data.local.dao.ChannelDao> { channelDao },
            javax.inject.Provider<com.tpsk.tj.chat.data.local.dao.ContactDao> { contactDao },
            javax.inject.Provider<com.tpsk.tj.chat.data.local.dao.SyncCursorDao> { syncCursorDao },
            javax.inject.Provider<com.tpsk.tj.chat.data.local.dao.CallLogDao> { callLogDao },
            sessionManager,
            javax.inject.Provider<com.tpsk.tj.common.data.local.AppDatabase> { appDatabase },
            webSocketManager,
            displayNameResolver,
            context
        )
        val chatId = "chat1"
        val message = Message(id = "msg1", conversationId = chatId, content = "Hello", senderId = "1", senderName = "User", timestamp = 123L)
        
        coEvery { chatDao.insertMessages(any()) } just Runs
        
        repository.saveMessage(chatId, message)
        
        coVerify { 
            chatDao.insertMessages(any())
        }
    }
}
