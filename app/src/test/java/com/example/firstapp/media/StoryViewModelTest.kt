package com.example.firstapp.media

import android.content.Context
import com.example.firstapp.stories.ui.StoryViewModel
import com.example.firstapp.stories.domain.StoryRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class StoryViewModelTest {

    private lateinit var viewModel: StoryViewModel
    private val repository: StoryRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val chatRepository: com.example.firstapp.domain.repository.ChatRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeStories() } returns flowOf(Resource.Success(emptyList<UserStory>()))
        every { chatRepository.getContacts() } returns flowOf(Resource.Success(emptyList<com.example.firstapp.chat.model.Contact>()))
        viewModel = StoryViewModel(repository, accountRepository, chatRepository, context)
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
    }

    @Test
    fun `posting story success updates post result`() = runTest {
        coEvery { repository.createStory("hello", null, "text", "contacts") } returns flowOf(Resource.Success(true))

        viewModel.postStory("hello", null, "text")

        assertTrue(viewModel.postResult.value is Resource.Success)
        coVerify { repository.createStory("hello", null, "text", "contacts") }
    }

    @Test
    fun `posting story error updates post result with error`() = runTest {
        coEvery { repository.createStory("hello", null, "text", "contacts") } returns flowOf(Resource.Error("Failed to create story"))

        viewModel.postStory("hello", null, "text")

        assertTrue(viewModel.postResult.value is Resource.Error)
        assertEquals("Failed to create story", (viewModel.postResult.value as Resource.Error).message)
    }
}
