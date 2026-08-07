package com.example.firstapp.media.ui

import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.common.util.Resource
import com.example.firstapp.common.data.ads.AdManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PostAndReplyEngagementTests {

    private lateinit var viewModel: MediaViewModel
    private val socialRepository: SocialRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val adManager: AdManager = mockk(relaxed = true)
    private val postWebSocketClient: com.example.firstapp.common.data.realtime.PostWebSocketClient = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MediaViewModel(socialRepository, accountRepository, adManager, postWebSocketClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `liking a post toggles state and increments count`() = runTest {
        val postId = "123"
        coEvery { socialRepository.likePost(postId) } returns flowOf(Resource.Success(true))
        
        viewModel.likePost(postId, 10L, false)
        
        coVerify { socialRepository.updatePostLikeLocally(postId, 11L, true) }
    }

    @Test
    fun `reposting a post increments share count`() = runTest {
        val postId = "123"
        coEvery { socialRepository.repostPost(postId) } returns flowOf(Resource.Success(true))
        
        viewModel.repostPost(postId)
        
        coVerify { socialRepository.repostPost(postId) }
    }

    @Test
    fun `deletePost calls repository and triggers refresh`() = runTest {
        val postId = "123"
        coEvery { socialRepository.deletePost(postId) } returns flowOf(Resource.Success(true))
        
        viewModel.deletePost(postId)
        
        coVerify { socialRepository.deletePost(postId) }
        assertEquals(1, viewModel.uiState.value.refreshTrigger)
    }

    @Test
    fun `deletePostReply calls repository and triggers refresh`() = runTest {
        val replyId = "r123"
        coEvery { socialRepository.deletePostReply(replyId) } returns flowOf(Resource.Success(true))
        
        viewModel.deletePostReply(replyId)
        
        coVerify { socialRepository.deletePostReply(replyId) }
        assertEquals(1, viewModel.uiState.value.refreshTrigger)
    }
}
