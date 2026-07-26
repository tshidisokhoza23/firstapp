package com.example.firstapp.media.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.media.data.repository.mapApiPost
import com.example.firstapp.media.model.Post
import retrofit2.HttpException
import java.io.IOException

class PostPagingSource(
    private val socialApi: SocialApi,
    private val following: Boolean = false,
    private val authorId: String,
    private val feedType: String = "standard"
) : PagingSource<Int, Post>() {

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val position = params.key ?: 1
        
        // Custom logic to wait if needed can't be easily done here without a reference to the ad status.
        // But we can ensure the first load is large enough or subsequent loads are triggered carefully.

        return try {
            val response = if (feedType == "short" && authorId == "") {
                socialApi.getShorts(page = position)
            } else {
                socialApi.getPosts(page = position, following = following, authorId = authorId, feedType = feedType)
            }

            if (!response.isSuccessful) {
                return LoadResult.Error(HttpException(response))
            }

            val posts = response.body()?.map { mapApiPost(it) } ?: emptyList()

            LoadResult.Page(
                data = posts,
                prevKey = if (position == 1) null else position - 1,
                nextKey = if (posts.isEmpty()) null else position + 1
            )
        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        }
    }
}
