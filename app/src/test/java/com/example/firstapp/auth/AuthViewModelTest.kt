package com.example.firstapp.auth

import com.example.firstapp.auth.ui.viewmodel.AuthViewModel
import com.example.firstapp.common.data.api.AuthResponse
import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.Resource
import com.example.firstapp.vocab.domain.repository.UserRepository
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
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `login with correct credentials navigates to OTP screen`() = runTest {
        val user = User(id = "1", username = "test", display_name = "Test", phone_number = "1234567890")
        val authResponse = AuthResponse(token = "token", user = user)
        
        coEvery { userRepository.login("test", "password") } returns flowOf(Resource.Success(authResponse))
        coEvery { userRepository.requestOTP("1234567890") } returns flowOf(Resource.Success("OTP Sent"))

        viewModel.login("test", "password")

        assertEquals("1234567890", viewModel.lastRequestedPhone)
        coVerify { userRepository.requestOTP("1234567890") }
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
}
