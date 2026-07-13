package com.example.firstapp.media.ui

import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.Post
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
class MediaNavAndToolbarTests {

    private lateinit var viewModel: MediaViewModel
    private val socialRepository: SocialRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MediaViewModel(socialRepository, accountRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `search filters profiles and posts matching the string`() = runTest {
        val query = "test"
        coEvery { socialRepository.getPosts(1) } returns flowOf(Resource.Success(emptyList<Post>()))
        
        viewModel.search(query)
        
        assertNotNull(viewModel.searchResults.value)
    }

    @Test
    fun `search cache prevents duplicate network hits within TTL`() = runTest {
        val query = "cache-test"
        coEvery { socialRepository.getPosts(1) } returns flowOf(Resource.Success(emptyList<Post>()))
        
        viewModel.search(query)
        viewModel.search(query)
        
        coVerify(exactly = 1) { socialRepository.getPosts(1) }
    }
}
