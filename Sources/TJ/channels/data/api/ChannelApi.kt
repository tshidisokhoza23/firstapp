package com.tpsk.tj.channels.data.api

import com.tpsk.tj.common.data.api.MuteRequest
import com.tpsk.tj.common.data.api.StatusResponse
import com.tpsk.tj.chat.data.api.ApiChannel
import com.tpsk.tj.chat.data.api.ChannelsResponse
import com.tpsk.tj.chat.data.api.CreateChannelRequest
import com.tpsk.tj.chat.data.api.ToggleAdminPostingRequest
import com.tpsk.tj.media.data.api.UserIdRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChannelApi {
    @GET("channels")
    suspend fun getChannels(): Response<ChannelsResponse>

    @GET("chats/channels")
    suspend fun getChatsChannels(): Response<ChannelsResponse>

    @GET("chats/channels/search")
    suspend fun searchChannels(@Query("query") query: String): Response<ChannelsResponse>

    @GET("chats/channels/{channelId}")
    suspend fun getChannel(@Path("channelId") channelId: String): Response<ApiChannel>

    @POST("chats/channels")
    suspend fun createChannel(@Body request: CreateChannelRequest): Response<ApiChannel>

    @PATCH("chats/channels/{channelId}/avatar")
    suspend fun updateChannelAvatar(
        @Path("channelId") channelId: String,
        @Body request: com.tpsk.tj.chat.data.api.AvatarUrlRequest
    ): Response<StatusResponse>

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
