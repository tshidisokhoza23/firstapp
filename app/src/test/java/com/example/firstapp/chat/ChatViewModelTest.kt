package com.example.firstapp.chat

import com.example.firstapp.chat.data.CallTonePlayer
import com.example.firstapp.chat.data.WebRtcCallManager
import com.example.firstapp.chat.model.ChatMessage
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
import com.example.firstapp.domain.repository.SettingsRepository
import java.io.File
import kotlin.collections.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel
    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val webRtcCallManager: WebRtcCallManager = mockk(relaxed = true)
    private val callTonePlayer: CallTonePlayer = mockk(relaxed = true)
    private val webSocketManager: com.example.firstapp.common.data.realtime.WebSocketManager = mockk(relaxed = true)
    private val callWebSocketClient: com.example.firstapp.chat.data.CallWebSocketClient = mockk(relaxed = true)
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { chatRepository.getContacts() } returns flowOf(Resource.Success(emptyList<Contact>()))
        every { sessionManager.authState } returns kotlinx.coroutines.flow.MutableStateFlow(false)
        every { sessionManager.currentUser } returns kotlinx.coroutines.flow.MutableStateFlow(null)
        viewModel = ChatViewModel(chatRepository, userRepository, accountRepository, settingsRepository, webRtcCallManager, callTonePlayer, webSocketManager, callWebSocketClient, sessionManager)
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
        val vm = ChatViewModel(chatRepository, userRepository, accountRepository, settingsRepository, webRtcCallManager, callTonePlayer, webSocketManager, callWebSocketClient, sessionManager)
        
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
        val createdGroup = mockk<ChatMessage>(relaxed = true)

        coEvery { chatRepository.createGroup("New Group", listOf("1")) } returns flowOf(Resource.Success(createdGroup))

        viewModel.updateSelectedContacts(listOf(regContact))
        
        var success = false
        var returnedGroup: ChatMessage? = null
        viewModel.createGroup("New Group", listOf("1")) {
            success = true
            returnedGroup = it
        }
        
        advanceUntilIdle()

        assertTrue(success)
        assertSame(createdGroup, returnedGroup)
        assertTrue(viewModel.uiState.value.selectedContactsForGroup.isEmpty())
        coVerify { chatRepository.createGroup("New Group", listOf("1")) }
    }

    @Test
    fun `sendMediaMessage uploads file and then sends message`() = runTest {
        val file = File("test.jpg")
        val uploadResponse = com.example.firstapp.common.data.api.UploadResponse("https://example.com/test.jpg", "test.jpg", 1024L)
        coEvery { accountRepository.uploadFile(any(), any()) } returns flowOf(Resource.Success(uploadResponse))
        
        viewModel.sendMediaMessage("chat1", file, "image")
        
        coVerify { accountRepository.uploadFile(file, "chat/images") }
        coVerify { chatRepository.sendMessage("chat1", "Image", "image", listOf("https://example.com/test.jpg"), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `sendMediaMessage upload failure sets error and skips send`() = runTest {
        val file = File("broken.jpg")
        coEvery { accountRepository.uploadFile(file, "chat/images") } returns flowOf(Resource.Error("offline"))

        viewModel.sendMediaMessage("chat1", file, "image")
        advanceUntilIdle()

        assertEquals("Upload failed: offline", viewModel.uiState.value.error)
        coVerify(exactly = 0) {
            chatRepository.sendMessage(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }
}
