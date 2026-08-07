package com.example.firstapp.media.ui

import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.common.domain.repository.SystemRepository
import android.content.Context
import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.Resource
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileTests {

    private lateinit var viewModel: UserViewModel
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val chatRepository: ChatRepository = mockk(relaxed = true)
    private val systemRepository: SystemRepository = mockk(relaxed = true)
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private val currentUserFlow = kotlinx.coroutines.flow.MutableStateFlow<User?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { sessionManager.currentUser } returns currentUserFlow
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg<ByteArray>())
        }
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            java.util.Base64.getDecoder().decode(firstArg<String>())
        }
        viewModel = UserViewModel(userRepository, accountRepository, chatRepository, systemRepository, sessionManager, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `profile editing saves and reflects instantly`() = runTest {
        val updatedUser = User(id = "1", displayName = "New Name", username = "newuser")
        coEvery { userRepository.updateUser(any(), any(), any(), any(), any(), any()) } returns flowOf(Resource.Success(updatedUser))
        
        // Simulating sessionManager update as ViewModel relies on it
        coEvery { userRepository.updateUser(any(), any(), any(), any(), any(), any()) } answers {
            currentUserFlow.value = updatedUser
            flowOf(Resource.Success(updatedUser))
        }

        viewModel.updateProfile("New Name", "Bio", "Location", null, null)

        assertEquals("New Name", viewModel.currentUser.value?.displayName)
    }
}
