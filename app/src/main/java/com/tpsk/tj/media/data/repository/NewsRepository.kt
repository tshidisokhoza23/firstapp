package com.tpsk.tj.media.data.repository

import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.common.util.formatTwitterTime
import com.tpsk.tj.media.data.api.NewsApi
import com.tpsk.tj.media.ui.screens.NewsArticle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface NewsRepository {
    fun getNews(category: String? = null, cursor: String? = null, limit: Int = 20): Flow<Resource<Pair<List<NewsArticle>, String?>>>
    fun toggleLike(articleId: String): Flow<Resource<Boolean>>
}

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val newsApi: NewsApi
) : NewsRepository {

    override fun getNews(category: String?, cursor: String?, limit: Int): Flow<Resource<Pair<List<NewsArticle>, String?>>> = flow {
        emit(Resource.Loading())
        try {
            val catParam = if (category.isNullOrBlank() || category.equals("All", ignoreCase = true)) null else category.lowercase()
            val response = newsApi.getNews(category = catParam, cursor = cursor, limit = limit)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val apiArticles = body.articles ?: emptyList()
                val nextCursor = body.next_cursor
                val articles = apiArticles.map { api ->
                    NewsArticle(
                        id = api.id,
                        title = api.title,
                        sourceName = api.source,
                        timeAgo = formatTwitterTime(api.published_at),
                        imageUrl = api.image_url.orEmpty().ifBlank {
                            "https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=800"
                        },
                        category = api.category,
                        summary = api.summary,
                        originalUrl = api.original_url,
                        commentCount = api.comment_count,
                        likeCount = api.like_count,
                        isLiked = api.has_liked ?: false
                    )
                }
                emit(Resource.Success(Pair(articles, nextCursor)))
            } else {
                emit(Resource.Error("Could not load news", Pair(emptyList(), null)))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error", Pair(emptyList(), null)))
        }
    }

    override fun toggleLike(articleId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = newsApi.toggleLike(articleId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.has_liked))
            } else {
                emit(Resource.Error("Could not toggle like"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }
}
