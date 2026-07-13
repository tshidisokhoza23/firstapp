package com.example.firstapp.media

import com.example.firstapp.media.ui.viewmodel.ChannelsViewModel
import com.example.firstapp.media.domain.repository.ChannelRepository
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
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelsViewModelTest {

    private lateinit var viewModel: ChannelsViewModel
    private val repository: ChannelRepository = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getChannels() } returns flowOf(Resource.Success(emptyList()))
        viewModel = ChannelsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `owner can promote user to admin in channel`() = runTest {
        val channelId = "chan1"
        val userId = "user2"
        coEvery { repository.addAdmin(channelId, userId) } returns flowOf(Resource.Success(true))
        
        viewModel.addAdmin(channelId, userId)
        
        coVerify { repository.addAdmin(channelId, userId) }
    }
}
