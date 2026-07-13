package com.example.firstapp.media.ui

import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.media.domain.repository.ShortsRepository
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel
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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ShortsViewModelTest {

    private lateinit var viewModel: ShortsViewModel
    private val shortsRepository: ShortsRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ShortsViewModel(shortsRepository, accountRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uploadShort uploads video and creates entry`() = runTest {
        val videoFile = mockk<File>(relaxed = true)
        val videoResponse = com.example.firstapp.common.data.api.UploadResponse("https://video.com", "v.mp4", 500)
        
        coEvery { accountRepository.uploadFile(videoFile, "shorts/videos") } returns flowOf(Resource.Success(videoResponse))
        coEvery { shortsRepository.createShort(any(), any(), any()) } returns flowOf(Resource.Success("short123"))

        viewModel.uploadShort(videoFile, "Nice short", null)

        coVerify { accountRepository.uploadFile(videoFile, "shorts/videos") }
        coVerify { shortsRepository.createShort("Nice short", "https://video.com", "") }
        assertFalse(viewModel.isUploading.value)
    }
}
