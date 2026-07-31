package com.example.firstapp.media.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.common.data.local.entity.PostEntity
import com.example.firstapp.common.data.local.entity.PostRemoteKeys
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.media.data.repository.toPostEntity
import com.example.firstapp.media.data.repository.mapApiPost
import com.example.firstapp.media.data.repository.mapMediaFeedItems
import retrofit2.Response
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val socialApi: SocialApi,
    private val appDatabase: AppDatabase,
    private val following: Boolean = false,
    private val authorId: String,
    private val feedType: String = "standard"
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: 1
            }
            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                val prevKey = remoteKeys?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                prevKey
            }
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                nextKey
            }
        }

        try {
            android.util.Log.d("PostRemoteMediator", "Loading page $page, following=$following, authorId=$authorId, feedType=$feedType")
            
            val apiPosts = if (!following && authorId == "") {
                // ... logic for general feeds ...
                when (feedType) {
                    "short" -> {
                        val response = socialApi.getShorts(page = page)
                        ensureSuccessful(response)
                        response.body()?.map { mapApiPost(it) } ?: emptyList()
                    }
                    "media" -> {
                        val response = socialApi.getMediaFeed(page = page)
                        ensureSuccessful(response)
                        response.body()?.map { mapApiPost(it) } ?: emptyList()
                    }
                    else -> {
                        val limit = state.config.pageSize
                        val offset = (page - 1) * limit
                        val response = socialApi.getFeed(limit = limit, offset = offset)
                        ensureSuccessful(response)
                        mapMediaFeedItems(response.body()?.items ?: emptyList())
                    }
                }
            } else {
                val response = socialApi.getPosts(page = page, following = following, authorId = authorId, feedType = feedType)
                ensureSuccessful(response)
                response.body()?.map { mapApiPost(it) } ?: emptyList()
            }

            val endOfPaginationReached = apiPosts.isEmpty()

            appDatabase.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    appDatabase.postRemoteKeysDao().clearRemoteKeys(feedType)
                    appDatabase.postDao().clearPosts(feedType)
                }
                val prevKey = if (page == 1) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = apiPosts.map {
                    PostRemoteKeys(postId = it.id, feedType = feedType, prevKey = prevKey, nextKey = nextKey)
                }
                appDatabase.postRemoteKeysDao().insertAll(keys)
                val entities = apiPosts.map { it.toPostEntity(feedType) }
                appDatabase.postDao().insertPosts(entities)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, PostEntity>): PostRemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { post ->
                appDatabase.postRemoteKeysDao().remoteKeysPostId(post.id, feedType)
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, PostEntity>): PostRemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { post ->
                appDatabase.postRemoteKeysDao().remoteKeysPostId(post.id, feedType)
            }
    }

    private suspend fun getRemoteKeyClosestToPosition(
        state: PagingState<Int, PostEntity>
    ): PostRemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { postId ->
                appDatabase.postRemoteKeysDao().remoteKeysPostId(postId, feedType)
            }
        }
    }

    private fun ensureSuccessful(response: Response<*>) {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }
}
