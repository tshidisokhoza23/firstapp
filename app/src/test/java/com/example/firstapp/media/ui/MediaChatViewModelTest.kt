package com.example.firstapp.media.ui

import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.MessageType
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.media.ui.viewmodel.MediaChatViewModel
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.common.util.Resource
import com.example.firstapp.common.model.User
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.collections.listOf

@OptIn(ExperimentalCoroutinesApi::class)
class MediaChatViewModelTest {

    private lateinit var viewModel: MediaChatViewModel
    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MediaChatViewModel(chatRepository, userRepository, accountRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMessages initiates DM and fetches messages`() = runTest {
        val userId = "user1"
        val chatId = "chat123"
        val user = User(id = userId, username = "test", display_name = "Test User")
        val message = Message(id = "m1", content = "Hello", type = MessageType.TEXT)

        coEvery { userRepository.getUserData(userId) } returns flowOf(user)
        coEvery { chatRepository.initiateDM(userId) } returns flowOf(Resource.Success(chatId))
        coEvery { chatRepository.getMessages(chatId) } returns flowOf(Resource.Success(listOf(message)))

        viewModel.loadMessages(userId)

        assertEquals(user, viewModel.receiverUser.value)
        assertEquals(1, viewModel.messages.value.size)
        assertEquals("Hello", viewModel.messages.value[0].content)
    }

    @Test
    fun `sendMediaMessage uploads file and sends message`() = runTest {
        val userId = "user1"
        val chatId = "chat123"
        val file = mockk<File>(relaxed = true)
        val uploadResponse = com.example.firstapp.common.data.api.UploadResponse("https://url.com", "file.jpg", 100)

        // Pre-setup for chatId
        coEvery { chatRepository.initiateDM(userId) } returns flowOf(Resource.Success(chatId))
        viewModel.loadMessages(userId)

        coEvery { accountRepository.uploadFile(any(), any()) } returns flowOf(Resource.Success(uploadResponse))
        coEvery { chatRepository.sendMessage(any(), any(), any(), any()) } returns flowOf(Resource.Success(mockk()))

        viewModel.sendMediaMessage(userId, file, "image")

        coVerify { accountRepository.uploadFile(file, "media_chat") }
        coVerify { chatRepository.sendMessage(chatId, "", "image", "https://url.com") }
    }
}
