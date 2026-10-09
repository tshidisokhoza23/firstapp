package com.tpsk.tj.media.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.tpsk.tj.common.data.local.AppDatabase
import com.tpsk.tj.media.data.HiddenPosts
import com.tpsk.tj.media.data.local.entity.PostEntity
import com.tpsk.tj.media.data.local.entity.PostRemoteKeys
import com.tpsk.tj.media.model.PostType
import com.tpsk.tj.media.data.api.SocialApi
import com.tpsk.tj.media.data.repository.toPostEntity
import com.tpsk.tj.media.data.repository.mapApiPost
import com.tpsk.tj.media.data.repository.mapMediaFeedItems
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import retrofit2.HttpException

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val socialApi: SocialApi,
    private val databaseProvider: () -> AppDatabase,
    private val following: Boolean = false,
    private val authorId: String,
    private val apiFeedType: String = "standard",
    private val dbFeedType: String = "standard",
    private val categoryId: String? = null,
    private val qualityMode: String? = null
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
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

            android.util.Log.d("PostRemoteMediator", "Loading page $page, following=$following, authorId=$authorId, apiFeedType=$apiFeedType, categoryId=$categoryId")
            
            val apiPosts = if (!following && authorId == "") {
                when (apiFeedType) {
                    "short" -> {
                        val response = socialApi.getShorts(page = page, categoryId = categoryId)
                        ensureSuccessful(response)
                        val data = response.body() ?: emptyList()
                        val mapped = data.map { mapApiPost(it, "short") }
                        mapped.filter { it.type == PostType.SHORT && it.mediaUrls.isNotEmpty() }
                    }
                    "media" -> {
                        val response = socialApi.getMediaFeed(page = page, categoryId = categoryId)
                        ensureSuccessful(response)
                        val data = response.body() ?: emptyList()
                        data.map { mapApiPost(it) }
                    }
                    else -> {
                        val limit = state.config.pageSize
                        val offset = (page - 1) * limit
                        val response = socialApi.getFeed(limit = limit, offset = offset, categoryId = categoryId, quality = qualityMode)
                        ensureSuccessful(response)
                        val data = response.body()?.items ?: emptyList()
                        mapMediaFeedItems(data).filterNot { isServerAd(it.type, it.id) }
                    }
                }
            } else {
                val response = socialApi.getPosts(page = page, following = following, authorId = authorId, feedType = apiFeedType, categoryId = categoryId, quality = qualityMode)
                ensureSuccessful(response)
                val data = response.body() ?: emptyList()
                val mapped = data.map { mapApiPost(it, if (apiFeedType == "short") "short" else null) }
                if (apiFeedType == "short") {
                    mapped.filter { it.type == PostType.SHORT && it.mediaUrls.isNotEmpty() }
                } else {
                    mapped.filterNot { isServerAd(it.type, it.id) }
                }
            }

            val uniquePosts = apiPosts
                .filter { it.id.isNotBlank() && it.id !in HiddenPosts.ids && !isServerAd(it.type, it.id) }
                .distinctBy { it.id }
            val endOfPaginationReached = apiPosts.isEmpty()
            if (loadType == LoadType.REFRESH && uniquePosts.isEmpty()) {
                return MediatorResult.Success(endOfPaginationReached = false)
            }

            val appDatabase = databaseProvider()
            appDatabase.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    val existingPosts = appDatabase.postDao().getPostsList(dbFeedType)
                    val now = System.currentTimeMillis()
                    val incomingIds = uniquePosts.map { it.id }.toSet()
                    val newlyCreatedLocally = existingPosts.filter { entity ->
                        val ts = entity.timestamp.toLongOrNull() ?: 0L
                        entity.id.isNotBlank() &&
                            entity.id !in HiddenPosts.ids &&
                            entity.id !in incomingIds &&
                            (now - ts) < 120_000L
                    }
                    if (newlyCreatedLocally.isNotEmpty()) {
                        appDatabase.postDao().insertPosts(newlyCreatedLocally)
                    }
                    appDatabase.postDao().insertPosts(uniquePosts.map { it.toPostEntity(dbFeedType) })
                    val keepIds = incomingIds + newlyCreatedLocally.map { it.id }
                    if (keepIds.isNotEmpty()) {
                        appDatabase.postDao().deletePostsExcept(dbFeedType, keepIds.toList())
                    }
                    appDatabase.postRemoteKeysDao().clearRemoteKeys(dbFeedType)
                }
                if (uniquePosts.isNotEmpty()) {
                    val prevKey = if (page == 1) null else page - 1
                    val nextKey = if (endOfPaginationReached) null else page + 1
                    val keys = uniquePosts.map {
                        PostRemoteKeys(postId = it.id, feedType = dbFeedType, prevKey = prevKey, nextKey = nextKey)
                    }
                    appDatabase.postRemoteKeysDao().insertAll(keys)
                    val entities = uniquePosts.map { it.toPostEntity(dbFeedType) }
                    appDatabase.postDao().insertPosts(entities)
                }
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            android.util.Log.e("PostRemoteMediator", "Feed load failed", exception)
            if (isCorruptDatabase(exception)) {
                // Closed pre-login instance, or a file written before the key
                // existed. Drop it so the next open builds the logged-in DB.
                runCatching { AppDatabase.invalidateCurrentUser() }
            }
            return MediatorResult.Error(exception)
        }
    }

    private fun isServerAd(type: PostType, id: String): Boolean {
        if (type == PostType.AD_UNIT || type == PostType.EXTERNAL_AD_PLACEHOLDER || type == PostType.NATIVE_AD || type == PostType.INTERNAL_AD) {
            return true
        }
        return id.startsWith("ad_unit_") || id.startsWith("external_ad_") || id.startsWith("native-ad")
    }

    private fun isCorruptDatabase(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            val name = current.javaClass.simpleName
            val message = current.message.orEmpty()
            if (name.contains("NotADatabase") ||
                message.contains("file is not a database") ||
                message.contains("code 26")
            ) {
                    return true
            }
            current = current.cause
        }
        return false
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, PostEntity>): PostRemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { post ->
                databaseProvider().postRemoteKeysDao().remoteKeysPostId(post.id, dbFeedType)
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, PostEntity>): PostRemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { post ->
                databaseProvider().postRemoteKeysDao().remoteKeysPostId(post.id, dbFeedType)
            }
    }

    private suspend fun getRemoteKeyClosestToPosition(
        state: PagingState<Int, PostEntity>
    ): PostRemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { postId ->
                databaseProvider().postRemoteKeysDao().remoteKeysPostId(postId, dbFeedType)
            }
        }
    }

    private fun ensureSuccessful(response: Response<*>) {
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }
}
