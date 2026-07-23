package com.example.firstapp.media.ui.viewmodel

import androidx.paging.PagingData
import com.example.firstapp.media.model.Post
import com.example.firstapp.media.model.PostReply
import com.example.firstapp.media.data.api.TrendingTopic
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.util.Resource
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
    val shortReplies: Resource<List<PostReply>> = Resource.Success(emptyList()),
    val isUploading: Boolean = false
)
