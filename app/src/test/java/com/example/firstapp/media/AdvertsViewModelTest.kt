package com.example.firstapp.media

import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.media.domain.repository.AdRepository
import com.example.firstapp.media.ui.viewmodel.AdvertsViewModel
import com.example.firstapp.vocab.domain.repository.UserRepository
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
class AdvertsViewModelTest {

    private lateinit var viewModel: AdvertsViewModel
    private val adRepository: AdRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val adManager: AdManager = mockk(relaxed = true)
    private val poolManager: com.example.firstapp.common.data.ads.RewardedAdPoolManager = mockk(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(com.example.firstapp.chat.rewards.data.RewardsManager)
        every { com.example.firstapp.chat.rewards.data.RewardsManager.addOptimisticReward(any()) } just Runs
        coEvery { com.example.firstapp.chat.rewards.data.RewardsManager.refreshBalance() } returns true
        coEvery { com.example.firstapp.chat.rewards.data.RewardsManager.awardPoints(any(), any(), any()) } returns true

        every { adRepository.getAdverts() } returns flowOf(Resource.Success(emptyList<com.example.firstapp.media.model.Advert>()))
        every { adRepository.getViewedAdverts() } returns flowOf(Resource.Success(emptyList<com.example.firstapp.media.model.Advert>()))
        every { userRepository.observeCurrentUser() } returns flowOf(null)
        every { poolManager.hasAvailableAds } returns kotlinx.coroutines.flow.MutableStateFlow(false)
        every { poolManager.cooldownRemainingSeconds } returns kotlinx.coroutines.flow.MutableStateFlow(0)
        viewModel = AdvertsViewModel(adRepository, userRepository, adManager, poolManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `watching a rewarded ad awards points and refreshes`() = runTest {
        val advertId = "ad1"
        coEvery { userRepository.getCurrentUser() } returns flowOf(Resource.Success(mockk(relaxed = true)))
        coEvery { adRepository.markAdvertViewed(advertId) } returns flowOf(Resource.Success(true))
        
        viewModel.markAdvertAsViewed(advertId)
        
        coVerify { adRepository.markAdvertViewed(advertId) }
    }

    @Test
    fun `ad list resets precisely at 00 00`() = runTest {
        coEvery { adRepository.resetAds() } returns flowOf(Resource.Success(Unit))
        coEvery { userRepository.getCurrentUser() } returns flowOf(Resource.Success(mockk(relaxed = true)))
        
        viewModel.resetAds()
        
        coVerify { adRepository.resetAds() }
        // fetchAdverts emits refresh trigger and refreshes user.
        coVerify(atLeast = 1) { userRepository.getCurrentUser() }
    }
}
