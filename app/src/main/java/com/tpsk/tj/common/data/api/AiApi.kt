package com.tpsk.tj.common.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AiApi {
    @POST("ai/assist")
    suspend fun runAiAssist(
        @Body request: AiAssistRequest
    ): Response<AiAssistResponse>

    @GET("ai/smart-replies")
    suspend fun getSmartReplies(
        @Query("post_id") postId: String
    ): Response<SmartRepliesResponse>

    @POST("ai/artist-helper")
    suspend fun runArtistHelper(
        @Body request: ArtistHelperRequest
    ): Response<ArtistHelperResponse>
}

data class AiAssistRequest(
    @SerializedName("action") val action: String,
    @SerializedName("text") val text: String,
    @SerializedName("tone_preference") val tonePreference: String = "Casual"
)

data class AiAssistResponse(
    @SerializedName("status") val status: String = "success",
    @SerializedName("action") val action: String = "",
    @SerializedName("original_text") val originalText: String = "",
    @SerializedName("suggested_text") val suggestedText: String = "",
    @SerializedName("suggested_hashtags") val suggestedHashtags: List<String> = emptyList(),
    @SerializedName("thread_parts") val threadParts: List<String> = emptyList(),
    @SerializedName("tokens_used") val tokensUsed: Int = 0,
    @SerializedName("daily_limit_remaining") val dailyLimitRemaining: Int = 10,
    @SerializedName("source") val source: String = ""
)

data class SmartRepliesResponse(
    @SerializedName("post_id") val postId: String = "",
    @SerializedName("suggestions") val suggestions: List<String> = emptyList()
)

data class ArtistHelperRequest(
    @SerializedName("task_type") val taskType: String, // "process_helper", "series_naming", "commission_writer", "caption_generator"
    @SerializedName("title") val title: String? = null,
    @SerializedName("current_stage") val currentStage: String? = null,
    @SerializedName("theme") val theme: String? = null,
    @SerializedName("medium") val medium: String? = null,
    @SerializedName("price") val price: String? = null,
    @SerializedName("story") val story: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class ArtistHelperResponse(
    @SerializedName("result") val result: String = "",
    @SerializedName("suggestions") val suggestions: List<String> = emptyList()
)
