package com.tpsk.tj.media.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.data.ads.AdManager
import com.tpsk.tj.common.data.ads.AdUnitConfig
import com.tpsk.tj.common.data.ads.NativeAdPool
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.data.repository.NewsRepository
import com.tpsk.tj.media.ui.screens.NewsArticle
import com.tpsk.tj.media.ui.screens.NewsFeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepository,
    private val adManager: AdManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _articles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val articles: StateFlow<List<NewsArticle>> = _articles.asStateFlow()

    private val _feedItems = MutableStateFlow<List<NewsFeedItem>>(emptyList())
    val feedItems: StateFlow<List<NewsFeedItem>> = _feedItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPaginationLoading = MutableStateFlow(false)
    val isPaginationLoading: StateFlow<Boolean> = _isPaginationLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var nextCursor: String? = null
    private var canLoadMore = true

    init {
        // Pre-warm ad pool asynchronously in background
        NativeAdPool.warm(adManager, context, AdUnitConfig.NATIVE_POSTS_ID, portrait = false, target = 3)
        loadInitialNews()
    }

    fun loadInitialNews() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            nextCursor = null
            canLoadMore = true

            newsRepository.getNews(cursor = null, limit = 20).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _isLoading.value = true
                    }
                    is Resource.Success -> {
                        _isLoading.value = false
                        val (list, cursor) = resource.data ?: Pair(emptyList(), null)
                        nextCursor = cursor
                        canLoadMore = !cursor.isNullOrBlank() && list.isNotEmpty()
                        _articles.value = list
                        updateFeedItems(list)
                    }
                    is Resource.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = resource.message ?: "Failed to load news"
                        val (list, _) = resource.data ?: Pair(emptyList(), null)
                        _articles.value = list
                        updateFeedItems(list)
                    }
                }
            }
        }
    }

    fun loadMoreNews() {
        if (_isPaginationLoading.value || !canLoadMore || nextCursor.isNullOrBlank()) return

        viewModelScope.launch {
            _isPaginationLoading.value = true
            val currentCursor = nextCursor

            newsRepository.getNews(cursor = currentCursor, limit = 20).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _isPaginationLoading.value = true
                    }
                    is Resource.Success -> {
                        _isPaginationLoading.value = false
                        val (newList, cursor) = resource.data ?: Pair(emptyList(), null)
                        nextCursor = cursor
                        canLoadMore = !cursor.isNullOrBlank() && newList.isNotEmpty()

                        val combined = (_articles.value + newList).distinctBy { it.id }
                        _articles.value = combined
                        updateFeedItems(combined)
                    }
                    is Resource.Error -> {
                        _isPaginationLoading.value = false
                    }
                }
            }
        }
    }

    private fun updateFeedItems(articlesList: List<NewsArticle>) {
        val items = mutableListOf<NewsFeedItem>()
        articlesList.forEachIndexed { index, article ->
            items.add(NewsFeedItem.ArticleItem(article))
            // Insert pre-warmed native ad every 6 articles IF available in NativeAdPool
            if ((index + 1) % 6 == 0) {
                val preloadedAd = NativeAdPool.take(adManager, context, AdUnitConfig.NATIVE_POSTS_ID, portrait = false)
                if (preloadedAd != null) {
                    items.add(NewsFeedItem.NativeAdItem(preloadedAd))
                }
            }
        }
        _feedItems.value = items
        // Replenish pool in background
        NativeAdPool.warm(adManager, context, AdUnitConfig.NATIVE_POSTS_ID, portrait = false, target = 3)
    }

    fun toggleLike(articleId: String) {
        viewModelScope.launch {
            val currentList = _articles.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == articleId }
            if (index != -1) {
                val current = currentList[index]
                val newLiked = !current.isLiked
                val newLikes = if (newLiked) current.likeCount + 1 else (current.likeCount - 1).coerceAtLeast(0)
                currentList[index] = current.copy(isLiked = newLiked, likeCount = newLikes)
                _articles.value = currentList
                updateFeedItems(currentList)

                newsRepository.toggleLike(articleId).collect { }
            }
        }
    }
}
