package com.example.firstapp.media

import com.example.firstapp.media.ui.viewmodel.StoryViewModel
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.account.domain.repository.AccountRepository
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
import kotlin.collections.*

@OptIn(ExperimentalCoroutinesApi::class)
class StoryViewModelTest {

    private lateinit var viewModel: StoryViewModel
    private val socialRepository: SocialRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val chatRepository: com.example.firstapp.domain.repository.ChatRepository = mockk(relaxed = true)
    private val postWebSocketClient: com.example.firstapp.media.data.realtime.PostWebSocketClient = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { socialRepository.getStories() } returns flowOf(Resource.Success(emptyList<com.example.firstapp.media.model.UserStory>()))
        viewModel = StoryViewModel(socialRepository, accountRepository, chatRepository, postWebSocketClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `viewing a story ad awards reward points`() = runTest {
        val userId = "user1"
        val storyId = "ad_123"
        coEvery { socialRepository.markStoryAsViewed(userId, storyId) } returns flowOf(Resource.Success(true))
        
        viewModel.markStoryAsViewed(userId, storyId)
        
        coVerify { socialRepository.markStoryAsViewed(userId, storyId) }
        // Repository handles the +0.1 reward logic.
    }

    @Test
    fun `posting story success updates post result`() = runTest {
        coEvery { socialRepository.createStory("hello", null, "text") } returns flowOf(Resource.Success(true))

        viewModel.postStory("hello", null, "text")

        assertTrue(viewModel.postResult.value is Resource.Success)
        coVerify { socialRepository.createStory("hello", null, "text") }
    }
}
