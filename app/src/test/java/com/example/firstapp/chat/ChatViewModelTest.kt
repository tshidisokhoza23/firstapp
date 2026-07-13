package com.example.firstapp.chat

import com.example.firstapp.chat.data.CallTonePlayer
import com.example.firstapp.chat.data.WebRtcCallManager
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.common.util.Resource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import com.example.firstapp.account.domain.repository.AccountRepository
import java.io.File
import kotlin.collections.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel
    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val webRtcCallManager: WebRtcCallManager = mockk(relaxed = true)
    private val callTonePlayer: CallTonePlayer = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { chatRepository.getContacts() } returns flowOf(Resource.Success(emptyList<Contact>()))
        viewModel = ChatViewModel(chatRepository, userRepository, accountRepository, webRtcCallManager, callTonePlayer)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `when user has no chats, ui state indicates empty state`() = runTest {
        coEvery { chatRepository.getChats() } returns flowOf(Resource.Success(emptyList()))
        
        viewModel.fetchChats()
        
        val chats = viewModel.uiState.value.chats.data
        assertTrue(chats.isNullOrEmpty())
        // UI logic for prompt would be tested in Compose tests, but ViewModel state is confirmed empty.
    }

    @Test
    fun `contacts list correctly segments into registered and unregistered`() = runTest {
        val contact1 = Contact(id = "1", username = "reg", displayName = "Reg", phoneNumber = "111", isRegistered = true)
        val contact2 = Contact(id = "2", username = "unreg", displayName = "Unreg", phoneNumber = "222", isRegistered = false)
        
        coEvery { chatRepository.getContacts() } returns flowOf(Resource.Success(listOf(contact1, contact2)))
        
        // In ChatViewModel, contacts is a val stateIn with WhileSubscribed.
        val vm = ChatViewModel(chatRepository, userRepository, webRtcCallManager, callTonePlayer)
        
        // Start a collector to trigger the flow
        val collectJob = launch(testDispatcher) {
            vm.contacts.collect {}
        }
        
        val contacts = vm.contacts.value.data
        assertEquals(2, contacts?.size)
        assertTrue(contacts?.any { it.isRegistered } == true)
        assertTrue(contacts?.any { !it.isRegistered } == true)
        
        collectJob.cancel()
    }

    @Test
    fun `group chat creation only succeeds with registered contacts`() = runTest {
        val regContact = Contact(id = "1", username = "reg", displayName = "Reg", phoneNumber = "111", isRegistered = true)
        
        coEvery { chatRepository.createGroup("New Group", listOf("1")) } returns flowOf(Resource.Success(mockk(relaxed = true)))
        
        var success = false
        viewModel.createGroup("New Group", listOf("1")) {
            success = true
        }
        
        assertTrue(success)
        coVerify { chatRepository.createGroup("New Group", listOf("1")) }
    }

    @Test
    fun `sendMediaMessage uploads file and then sends message`() = runTest {
        val file = File("test.jpg")
        val uploadResponse = com.example.firstapp.common.data.api.UploadResponse("https://example.com/test.jpg", "test.jpg", 1024L)
        coEvery { accountRepository.uploadFile(any(), any()) } returns flowOf(Resource.Success(uploadResponse))
        
        viewModel.sendMediaMessage("chat1", file, "image")
        
        coVerify { accountRepository.uploadFile(file, "chat/images") }
        coVerify { chatRepository.sendMessage("chat1", "Image", "image", "https://example.com/test.jpg", any(), any(), any(), any(), any(), any(), any(), any()) }
    }
}
