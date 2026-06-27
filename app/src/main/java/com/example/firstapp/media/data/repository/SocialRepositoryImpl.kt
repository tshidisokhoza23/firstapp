package com.example.firstapp.media.data.repository

import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.media.data.api.ApiPostAuthor
import com.example.firstapp.common.data.local.dao.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.media.data.paging.PostRemoteMediator
import com.example.firstapp.common.ui.theme.AppPrimary
import com.example.firstapp.common.ui.theme.AppAccent
import androidx.compose.ui.graphics.Color
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.*
import android.util.Log
import javax.inject.Inject

class SocialRepositoryImpl @Inject constructor(
    private val socialApi: SocialApi,
    private val postDao: PostDao,
    private val storyDao: StoryDao,
    private val appDatabase: AppDatabase
) : SocialRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getPostsPaging(following: Boolean): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            remoteMediator = PostRemoteMediator(socialApi, appDatabase, following),
            pagingSourceFactory = { postDao.getPostsPaging() }
        ).flow.map { pagingData ->
            pagingData.map { it.toModel() }
        }
    }

    override suspend fun updatePostLikeLocally(postId: Int, likes: String, isLiked: Boolean) {
        postDao.updateLikeStatus(postId, likes, isLiked)
    }

    override fun getPosts(page: Int): Flow<Resource<List<Post>>> = flow {
        val cached = postDao.getPosts().first().map { it.toModel() }
        emit(Resource.Loading(cached))
        try {
            val response = socialApi.getPosts(page)
            if (response.isSuccessful) {
                val apiPosts = response.body()?.posts ?: emptyList()
                val posts = apiPosts.map { mapApiPost(it) }
                if (page == 1) postDao.clearPosts()
                postDao.insertPosts(posts.map { it.toEntity() })
            }
        } catch (e: Exception) { 
            Log.e("SocialRepositoryImpl", "getPosts failed", e)
        }
        postDao.getPosts().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun getFollowingPosts(page: Int): Flow<Resource<List<Post>>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.getPosts(page, following = true)
            if (response.isSuccessful) {
                val posts = response.body()?.posts?.map { mapApiPost(it) } ?: emptyList()
                emit(Resource.Success(posts))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error"))
        }
    }

    override fun createPost(content: String, mediaUrls: List<String>, postType: String?, tags: List<String>): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.createPost(com.example.firstapp.media.data.api.CreatePostRequest(content, mediaUrls, postType ?: "text_only", null, tags)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    
    override fun likePost(postId: Int): Flow<Resource<Boolean>> = flow {
        try {
            val response = socialApi.likePost(postId)
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Offline")) }
    }

    override fun repostPost(postId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.repostPost(postId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun bookmarkPost(postId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.bookmarkPost(postId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun createPostReply(postId: Int, content: String, parentReplyId: Int?): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.createPostReply(postId, com.example.firstapp.media.data.api.CreatePostReplyRequest(content, parentReplyId)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun likePostReply(replyId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.likePostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun repostPostReply(replyId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.repostPostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun bookmarkPostReply(replyId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.bookmarkPostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun votePoll(postId: Int, optionId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.votePoll(postId, com.example.firstapp.media.data.api.VotePollRequest(optionId)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun followUser(userId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.followUser(userId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun unfollowUser(userId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.unfollowUser(userId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun getTrendingTopics(): Flow<Resource<List<com.example.firstapp.media.data.api.TrendingTopic>>> = flow { emit(Resource.Loading()); try { val response = socialApi.getTrendingTopics(); if (response.isSuccessful) emit(Resource.Success(response.body()?.topics ?: emptyList())) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun getSuggestedUsers(): Flow<Resource<List<Contact>>> = flow { emit(Resource.Loading()); try { val response = socialApi.getSuggestedUsers(); if (response.isSuccessful) emit(Resource.Success(response.body()?.contacts?.map { user -> Contact(user.id, user.username, user.display_name, user.avatar_url, "", false, true, user.is_verified, "") } ?: emptyList())) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }

    override fun getStories(): Flow<Resource<List<UserStory>>> = flow {
        val cachedEntities = storyDao.getStories().first()
        val cached = cachedEntities.map { it.toModel() }
        emit(Resource.Loading(cached))
        try {
            val response = socialApi.getStories()
            if (response.isSuccessful) {
                val apiStories = response.body()?.stories ?: emptyList()
                val stories = apiStories.map { mapApiStory(it) }
                storyDao.clearStories()
                storyDao.insertStories(stories.map { it.toEntity() })
            }
        } catch (_: Exception) {}
        
        storyDao.getStories().collect { entities ->
            val now = System.currentTimeMillis()
            val dayInMs = 24 * 60 * 60 * 1000L
            
            val models = entities.map { it.toModel() }.map { userStory: UserStory ->
                val filteredItems = userStory.stories.filter { item: StoryItem ->
                    item.createdAt == 0L || (now - item.createdAt < dayInMs)
                }.sortedBy { it.createdAt }

                userStory.copy(stories = filteredItems)
            }.filter { it.stories.isNotEmpty() }
            
            val sortedModels = models.sortedByDescending { story ->
                story.stories.maxOfOrNull { it.createdAt } ?: 0L
            }
            
            if (sortedModels.isEmpty()) {
                emit(Resource.Success(emptyList()))
            } else {
                emit(Resource.Success(sortedModels))
            }
        }
    }

    override fun createStory(content: String, contentUrl: String?, type: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = socialApi.createStory(com.example.firstapp.media.data.api.CreateStoryRequest(content, contentUrl, type))
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error: ${e.localizedMessage ?: "Connection failed"}"))
        }
    }

    override fun deleteStory(storyId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (socialApi.deleteStory(storyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun markStoryAsViewed(userId: Int, storyId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val stories = storyDao.getStories().first()
            val entity = stories.find { it.id == userId }
            if (entity != null) {
                val updatedItems = entity.stories.map { if (it.id == storyId) it.copy(isViewed = true) else it }
                storyDao.insertStories(listOf(entity.copy(stories = updatedItems)))
            }
            socialApi.markStoryViewed(storyId)
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun muteStory(userId: Int, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val stories = storyDao.getStories().first()
            val entity = stories.find { it.id == userId }
            if (entity != null) {
                storyDao.insertStories(listOf(entity.copy(isMuted = isMuted)))
            }
            socialApi.muteUser(userId, com.example.firstapp.common.data.api.MuteRequest(isMuted))
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun viewPost(postId: Int): Flow<Resource<Boolean>> = flow { try { socialApi.viewPost(postId); emit(Resource.Success(true)) } catch (_: Exception) {} }
    override fun deletePost(postId: Int): Flow<Resource<Boolean>> = flow { try { socialApi.deletePost(postId); emit(Resource.Success(true)) } catch (_: Exception) {} }

    override fun getStoryViewers(storyId: Int): Flow<Resource<List<com.example.firstapp.media.data.api.ApiViewer>>> = flow { emit(Resource.Loading()); try { val res = socialApi.getStoryViewers(storyId); if (res.isSuccessful) emit(Resource.Success(res.body()?.viewers ?: emptyList())) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Error")) } }
}
