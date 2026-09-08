package com.example.firstapp.channels.data.api

import com.example.firstapp.common.data.api.MuteRequest
import com.example.firstapp.common.data.api.StatusResponse
import com.example.firstapp.media.data.api.ApiChannel
import com.example.firstapp.media.data.api.ChannelsResponse
import com.example.firstapp.media.data.api.CreateChannelRequest
import com.example.firstapp.media.data.api.ToggleAdminPostingRequest
import com.example.firstapp.media.data.api.UserIdRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path

interface ChannelApi {
    @GET("channels")
    suspend fun getChannels(): Response<ChannelsResponse>

    @GET("chats/channels")
    suspend fun getChatsChannels(): Response<ChannelsResponse>

    @POST("chats/channels")
    suspend fun createChannel(@Body request: CreateChannelRequest): Response<ApiChannel>

    @POST("chats/channels/{channelId}/subscribe")
    suspend fun subscribeToChannel(@Path("channelId") channelId: String): Response<StatusResponse>

    @POST("chats/channels/{channelId}/mute")
    suspend fun muteChannel(@Path("channelId") channelId: String, @Body request: MuteRequest): Response<StatusResponse>

    @POST("chats/channels/{channelId}/admins")
    suspend fun addChannelAdmin(@Path("channelId") channelId: String, @Body request: UserIdRequest): Response<StatusResponse>

    @POST("chats/channels/{channelId}/admin-posting")
    suspend fun toggleAdminPosting(
        @Path("channelId") channelId: String,
        @Body request: ToggleAdminPostingRequest
    ): Response<StatusResponse>

    @HTTP(method = "DELETE", path = "chats/channels/{channelId}/admins", hasBody = true)
    suspend fun removeChannelAdmin(@Path("channelId") channelId: String, @Body request: UserIdRequest): Response<StatusResponse>
}
