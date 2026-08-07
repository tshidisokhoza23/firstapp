package com.example.firstapp.common

import com.example.firstapp.common.data.local.dao.ChatDao
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
    private val socialApi: com.example.firstapp.media.data.api.SocialApi = mockk(relaxed = true)
    private val userApi: com.example.firstapp.common.data.api.UserApi = mockk(relaxed = true)
    private val contactDao: com.example.firstapp.common.data.local.dao.ContactDao = mockk(relaxed = true)
    private val callLogDao: com.example.firstapp.common.data.local.dao.CallLogDao = mockk(relaxed = true)
    private val displayNameResolver: com.example.firstapp.common.util.DisplayNameResolver = mockk(relaxed = true)
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager = mockk(relaxed = true)
    private val chatWebSocketClient: com.example.firstapp.chat.data.ChatWebSocketClient = mockk(relaxed = true)
    private val context: android.content.Context = mockk(relaxed = true)

    @Test
    fun `chat cache prunes older messages to maintain LRU state`() = runTest {
        val repository = ChatRepositoryImpl(chatApi, socialApi, userApi, chatDao, contactDao, callLogDao, displayNameResolver, sessionManager, chatWebSocketClient, context)
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
