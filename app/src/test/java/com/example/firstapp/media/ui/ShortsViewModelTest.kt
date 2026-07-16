package com.example.firstapp.media.ui

import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.media.domain.repository.ShortsRepository
import com.example.firstapp.media.data.api.ShortLikeResponse
import com.example.firstapp.media.model.ShortMedia
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
import androidx.compose.ui.graphics.Color

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

    @Test
    fun `likeShort applies server synced count on success`() = runTest {
        val short = ShortMedia(
            id = "s1",
            creatorId = "u1",
            creatorName = "Alice",
            description = "desc",
            likes = "10",
            comments = "1",
            shares = "0",
            videoUrl = "https://v",
            thumbnailColor = Color.Black,
            profilePictureColor = Color.Gray,
            isLiked = false
        )
        coEvery { shortsRepository.getShorts() } returns flowOf(Resource.Success(listOf(short)))
        coEvery { shortsRepository.likeShort("s1") } returns flowOf(
            Resource.Success(ShortLikeResponse(status = "ok", is_liked = true, likes = 11))
        )

        viewModel.fetchShorts()
        viewModel.likeShort("s1")
        advanceUntilIdle()

        val updated = (viewModel.shorts.value as Resource.Success).data?.first()
        assertEquals("11", updated?.likes)
        assertTrue(updated?.isLiked == true)
    }

    @Test
    fun `likeShort reverts optimistic update on error`() = runTest {
        val short = ShortMedia(
            id = "s2",
            creatorId = "u2",
            creatorName = "Bob",
            description = "desc",
            likes = "4",
            comments = "0",
            shares = "0",
            videoUrl = "https://v2",
            thumbnailColor = Color.Black,
            profilePictureColor = Color.Gray,
            isLiked = false
        )
        coEvery { shortsRepository.getShorts() } returns flowOf(Resource.Success(listOf(short)))
        coEvery { shortsRepository.likeShort("s2") } returns flowOf(Resource.Error("network"))

        viewModel.fetchShorts()
        viewModel.likeShort("s2")
        advanceUntilIdle()

        val reverted = (viewModel.shorts.value as Resource.Success).data?.first()
        assertEquals("4", reverted?.likes)
        assertFalse(reverted?.isLiked == true)
    }
}
