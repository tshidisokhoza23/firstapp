package com.tpsk.tj.media.ui.viewmodel

import androidx.paging.PagingData
import com.tpsk.tj.media.model.Category
import com.tpsk.tj.media.model.DefaultCategories
import com.tpsk.tj.media.model.Post
import com.tpsk.tj.media.data.api.TrendingTopic
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class MediaUiState(
    val posts: Flow<PagingData<Post>> = flowOf(PagingData.empty()),
    val followingPosts: Flow<PagingData<Post>> = flowOf(PagingData.empty()),
    val trendingTopics: List<TrendingTopic> = emptyList(),
    val suggestedUsers: List<Contact> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedTab: Int = 0,
    val selectedPost: Post? = null,
    val refreshTrigger: Int = 0,
    val shortReplies: Resource<List<Post>> = Resource.Success(emptyList()),
    val isUploading: Boolean = false,
    val activePostId: String? = null,
    val categories: List<Category> = DefaultCategories,
    val selectedCategoryId: String? = null
)
