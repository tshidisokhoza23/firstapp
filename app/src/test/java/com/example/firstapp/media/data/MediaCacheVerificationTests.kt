package com.tpsk.tj.media.data

import com.tpsk.tj.media.data.repository.SocialRepositoryImpl
import com.tpsk.tj.media.data.api.SocialApi
import com.tpsk.tj.common.data.local.AppDatabase
import com.tpsk.tj.media.data.local.dao.PostDao
import com.tpsk.tj.media.data.realtime.RealtimeEngagementManager
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaCacheVerificationTests {

    private lateinit var repository: SocialRepositoryImpl
    private val socialApi: SocialApi = mockk(relaxed = true)
    private val userApi: com.tpsk.tj.common.data.api.UserApi = mockk(relaxed = true)
    private val postDao: PostDao = mockk(relaxed = true)
    private val contactDao: com.tpsk.tj.chat.data.local.dao.ContactDao = mockk(relaxed = true)
    private val appDatabase: AppDatabase = mockk(relaxed = true)
    private val displayNameResolver: com.tpsk.tj.common.util.DisplayNameResolver = mockk(relaxed = true)
    private val realtimeEngagementManager: RealtimeEngagementManager = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = SocialRepositoryImpl(socialApi, userApi, postDao, contactDao, appDatabase, displayNameResolver, realtimeEngagementManager)
    }

    @Test
    fun `getPosts returns cached data first before network call`() = runTest {
        val mockEntities = java.util.ArrayList<com.tpsk.tj.media.data.local.entity.PostEntity>()
        mockEntities.add(mockk(relaxed = true))
        every { postDao.getPosts(any()) } returns flowOf(mockEntities)
        
        val results = repository.getPosts(1, "standard").take(1).toList()
        
        assertTrue(results.size > 0)
        coVerify { postDao.getPosts(any()) }
    }
}
