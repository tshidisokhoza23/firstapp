package com.tpsk.tj.auth

import com.tpsk.tj.auth.ui.viewmodel.AuthViewModel
import com.tpsk.tj.common.data.api.AuthResponse
import com.tpsk.tj.common.model.User
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.vocab.domain.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.collections.*
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val sessionManager: com.tpsk.tj.common.domain.auth.SessionManager = mockk(relaxed = true)
    private val authApi: com.tpsk.tj.auth.data.api.AuthApi = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(userRepository, sessionManager, authApi)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `login with correct credentials updates auth state`() = runTest {
        val user = User(id = "1", username = "test", displayName = "Test", phoneNumber = "1234567890")
        val authResponse = AuthResponse(token = "token", user = user)
        
        coEvery { userRepository.login("test", "password") } returns flowOf(Resource.Success(authResponse))

        viewModel.login("test", "password")

        val state = viewModel.authState.value
        assertTrue(state is Resource.Success)
        assertEquals(user, (state as Resource.Success).data?.user)
        coVerify { sessionManager.saveSession("token", any(), user) }
    }

    @Test
    fun `login with invalid credentials throws error`() = runTest {
        coEvery { userRepository.login("test", "wrong") } returns flowOf(Resource.Error("Invalid credentials"))

        viewModel.login("test", "wrong")

        val state = viewModel.authState.value
        assertTrue(state is Resource.Error)
        assertEquals("Invalid credentials", state?.message)
    }

    @Test
    fun `registration form validation - field errors`() = runTest {
        viewModel.setFieldError("email", "Invalid email")
        assertEquals("Invalid email", viewModel.fieldErrors.value["email"])
        
        viewModel.setFieldError("email", null)
        assertNull(viewModel.fieldErrors.value["email"])
    }

    @Test
    fun `otp verification success triggers callback`() = runTest {
        coEvery { userRepository.verifyOTP("1234567890", "123456") } returns flowOf(Resource.Success(true))
        
        var callbackCalled = false
        viewModel.verifyOTP("1234567890", "123456") {
            callbackCalled = true
        }

        assertTrue(callbackCalled)
        assertEquals("Verified", (viewModel.otpState.value as Resource.Success).data)
    }

    @Test
    fun `otp verification failure sets error state`() = runTest {
        coEvery { userRepository.verifyOTP("1234567890", "000000") } returns flowOf(Resource.Error("Invalid code"))

        viewModel.verifyOTP("1234567890", "000000") {}

        val state = viewModel.otpState.value
        assertTrue(state is Resource.Error)
        assertEquals("Invalid code", state?.message)
    }

    @Test
    fun `register success updates auth state`() = runTest {
        val authResponse = AuthResponse(token = "token", user = User(id = "2", username = "newuser"))
        coEvery {
            userRepository.register("newuser", "new@example.com", "New User", "+27710000003", "password")
        } returns flowOf(Resource.Success(authResponse))

        viewModel.register("newuser", "new@example.com", "New User", "+27710000003", "password")

        val state = viewModel.authState.value
        assertTrue(state is Resource.Success)
    }
}
