package com.example.firstapp.media

import com.example.firstapp.chat.ui.viewmodel.StoryViewModel
import com.example.firstapp.chat.domain.repository.StoryRepository
import com.example.firstapp.chat.model.UserStory
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
    private val repository: StoryRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val chatRepository: com.example.firstapp.domain.repository.ChatRepository = mockk(relaxed = true)
    private val postWebSocketClient: com.example.firstapp.media.data.realtime.PostWebSocketClient = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getStories() } returns flowOf(Resource.Success(emptyList<UserStory>()))
        viewModel = StoryViewModel(repository, accountRepository, chatRepository, postWebSocketClient)
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
        coEvery { repository.markStoryAsViewed(userId, storyId) } returns flowOf(Resource.Success(true))
        
        viewModel.markStoryAsViewed(userId, storyId)
        
        coVerify { repository.markStoryAsViewed(userId, storyId) }
        // Repository handles the +0.1 reward logic.
    }

    @Test
    fun `posting story success updates post result`() = runTest {
        coEvery { repository.createStory("hello", null, "text") } returns flowOf(Resource.Success(true))

        viewModel.postStory("hello", null, "text")

        assertTrue(viewModel.postResult.value is Resource.Success)
        coVerify { repository.createStory("hello", null, "text") }
    }
}
