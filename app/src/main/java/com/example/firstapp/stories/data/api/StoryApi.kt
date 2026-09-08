package com.example.firstapp.stories.data.api

import com.example.firstapp.common.data.api.MuteRequest
import com.example.firstapp.common.data.api.StatusResponse
import com.example.firstapp.media.data.api.ApiViewer
import com.example.firstapp.media.data.api.CreateStoryRequest
import com.example.firstapp.media.data.api.CreateStoryResponse
import com.example.firstapp.media.data.api.StoriesResponse
import com.example.firstapp.media.data.api.ViewersResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StoryApi {
    @GET("chats/stories")
    suspend fun getStories(@Query("since") since: Long? = null): Response<StoriesResponse>

    @POST("chats/stories")
    suspend fun createStory(@Body request: CreateStoryRequest): Response<CreateStoryResponse>

    @POST("chats/stories/{storyId}/view")
    suspend fun markStoryViewed(@Path("storyId") storyId: String): Response<StatusResponse>

    @DELETE("chats/stories/{storyId}")
    suspend fun deleteStory(@Path("storyId") storyId: String): Response<StatusResponse>

    @GET("chats/stories/{storyId}/viewers")
    suspend fun getStoryViewers(@Path("storyId") storyId: String): Response<ViewersResponse>

    @POST("users/{userId}/mute")
    suspend fun muteUser(@Path("userId") userId: String, @Body request: MuteRequest): Response<StatusResponse>
}
