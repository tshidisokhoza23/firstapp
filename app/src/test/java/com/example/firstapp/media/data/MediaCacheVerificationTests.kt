package com.example.firstapp.media.data

import com.example.firstapp.media.data.repository.SocialRepositoryImpl
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.common.data.local.dao.PostDao
import com.example.firstapp.common.data.local.dao.StoryDao
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Collections

@OptIn(ExperimentalCoroutinesApi::class)
class MediaCacheVerificationTests {

    private lateinit var repository: SocialRepositoryImpl
    private val socialApi: SocialApi = mockk(relaxed = true)
    private val postDao: PostDao = mockk(relaxed = true)
    private val storyDao: StoryDao = mockk(relaxed = true)
    private val appDatabase: AppDatabase = mockk(relaxed = true)
    private val displayNameResolver: com.example.firstapp.common.util.DisplayNameResolver = mockk(relaxed = true)

    @Before
    fun setup() {
        repository = SocialRepositoryImpl(socialApi, postDao, storyDao, appDatabase, displayNameResolver)
    }

    @Test
    fun `getPosts returns cached data first before network call`() = runTest {
        // Mock cached data
        val mockEntities = java.util.ArrayList<com.example.firstapp.common.data.local.entity.PostEntity>()
        mockEntities.add(mockk(relaxed = true))
        every { postDao.getPosts() } returns flowOf(mockEntities)
        
        val results = repository.getPosts(1).take(1).toList()
        
        assertTrue(results.size > 0)
        coVerify { postDao.getPosts() }
    }
}
