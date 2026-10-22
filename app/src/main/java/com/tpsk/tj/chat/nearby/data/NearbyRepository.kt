package com.tpsk.tj.chat.nearby.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.tpsk.tj.chat.nearby.data.api.CreateNearbyPostRequest
import com.tpsk.tj.chat.nearby.data.api.CreateNearbyReplyRequest
import com.tpsk.tj.chat.nearby.data.api.NearbyApi
import com.tpsk.tj.chat.nearby.model.NearbyPost
import com.tpsk.tj.chat.nearby.model.NearbyReply
import com.tpsk.tj.common.util.Resource
import retrofit2.Response
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class NearbyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nearbyApi: NearbyApi
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nearby_location_prefs", Context.MODE_PRIVATE)
    private val reportedPostIds = mutableSetOf<String>()
    private val blockedUserIds = mutableSetOf<String>()
    private var lastPostTimestampMs: Long = 0L

    private val localPosts = MutableStateFlow<List<NearbyPost>>(emptyList())

    fun saveCoarseLocation(lat: Double, lng: Double, areaName: String) {
        val roundedLat = (lat * 100).roundToInt() / 100.0
        val roundedLng = (lng * 100).roundToInt() / 100.0
        prefs.edit()
            .putFloat("coarse_lat", roundedLat.toFloat())
            .putFloat("coarse_lng", roundedLng.toFloat())
            .putString("coarse_area_name", areaName)
            .putLong("cached_at_ms", System.currentTimeMillis())
            .apply()
    }

    fun getCachedCoarseAreaName(): String {
        return prefs.getString("coarse_area_name", null) ?: "Braamfontein"
    }

    fun getCachedCoarseLat(): Double {
        return prefs.getFloat("coarse_lat", -26.19f).toDouble()
    }

    fun getCachedCoarseLng(): Double {
        return prefs.getFloat("coarse_lng", 28.03f).toDouble()
    }

    fun getNearbyPosts(
        lat: Double,
        lng: Double,
        radiusKm: Int,
        selectedCity: String? = null
    ): Flow<Resource<List<NearbyPost>>> = flow {
        emit(Resource.Loading())
        try {
            val response = nearbyApi.getPosts(
                lat = if (lat != 0.0) lat else null,
                lng = if (lng != 0.0) lng else null,
                radius = radiusKm,
                area = selectedCity
            )
            if (response.isSuccessful && response.body() != null) {
                val apiPosts = response.body()!!.posts
                val mapped = apiPosts.map { api ->
                    NearbyPost(
                        id = api.id,
                        userId = api.user_id,
                        authorName = api.author_name,
                        authorAvatarUrl = api.author_avatar_url,
                        authorAvatarColor = Color(0xFF00B4D8),
                        content = api.content,
                        coarseAreaName = api.area_name,
                        distanceKm = api.distance_km,
                        likeCount = api.like_count,
                        replyCount = api.reply_count,
                        isLiked = api.is_liked,
                        createdAt = parseIsoTime(api.created_at),
                        replies = api.replies?.map { r ->
                            NearbyReply(
                                id = r.id,
                                postId = r.post_id,
                                userId = r.user_id,
                                authorName = r.author_name,
                                authorAvatarUrl = r.author_avatar_url,
                                authorAvatarColor = Color(0xFFE91E63),
                                content = r.content,
                                createdAt = parseIsoTime(r.created_at)
                            )
                        } ?: emptyList()
                    )
                }.filter { post ->
                    !reportedPostIds.contains(post.id) && !blockedUserIds.contains(post.userId)
                }
                localPosts.value = mapped
                emit(Resource.Success(mapped))
            } else {
                emit(Resource.Error(response.errorText("Unable to fetch nearby posts"), localPosts.value))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error fetching nearby posts", localPosts.value))
        }
    }

    fun createPost(
        content: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String? = null,
        areaName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Flow<Resource<NearbyPost>> = flow {
        val trimmed = content.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isBlank()) {
            emit(Resource.Error("Post content cannot be empty"))
            return@flow
        }
        if (trimmed.length > 500) {
            emit(Resource.Error("Post exceeds 500 characters limit"))
            return@flow
        }

        emit(Resource.Loading())
        try {
            val req = CreateNearbyPostRequest(
                content = trimmed,
                area_name = areaName?.takeIf { it.isNotBlank() } ?: getCachedCoarseAreaName(),
                latitude = latitude ?: getCachedCoarseLat(),
                longitude = longitude ?: getCachedCoarseLng()
            )
            val response = nearbyApi.createPost(req)
            if (response.isSuccessful && response.body() != null) {
                val api = response.body()!!
                val newPost = NearbyPost(
                    id = api.id,
                    userId = api.user_id,
                    authorName = if (api.author_name.isNotBlank() && api.author_name != "User") api.author_name else authorName,
                    authorAvatarUrl = api.author_avatar_url ?: authorAvatarUrl,
                    authorAvatarColor = Color(0xFF00B4D8),
                    content = api.content,
                    coarseAreaName = api.area_name,
                    distanceKm = 0.5,
                    likeCount = api.like_count,
                    replyCount = api.reply_count,
                    isLiked = false,
                    createdAt = parseIsoTime(api.created_at)
                )
                localPosts.value = listOf(newPost) + localPosts.value
                emit(Resource.Success(newPost))
            } else {
                emit(Resource.Error(response.errorText("Failed to publish nearby post")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error creating post"))
        }
    }

    fun likePost(postId: String): Flow<Resource<NearbyPost>> = flow {
        try {
            emit(Resource.Loading())
            val response = nearbyApi.toggleLike(postId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val updated = localPosts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(isLiked = body.is_liked, likeCount = body.like_count)
                    } else {
                        post
                    }
                }
                localPosts.value = updated
                updated.find { it.id == postId }?.let { emit(Resource.Success(it)) }
                    ?: emit(Resource.Error("Post not found"))
            } else {
                emit(Resource.Error("Could not toggle like on server"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error toggling like"))
        }
    }

    fun replyToPost(
        postId: String,
        replyContent: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String? = null
    ): Flow<Resource<NearbyReply>> = flow {
        val trimmed = replyContent.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isBlank()) {
            emit(Resource.Error("Reply cannot be empty"))
            return@flow
        }

        emit(Resource.Loading())
        try {
            val response = nearbyApi.createReply(postId, CreateNearbyReplyRequest(trimmed))
            if (response.isSuccessful && response.body() != null) {
                val r = response.body()!!
                val newReply = NearbyReply(
                    id = r.id,
                    postId = r.post_id,
                    userId = r.user_id,
                    authorName = if (r.author_name.isNotBlank() && r.author_name != "User") r.author_name else authorName,
                    authorAvatarUrl = r.author_avatar_url ?: authorAvatarUrl,
                    authorAvatarColor = Color(0xFFE91E63),
                    content = r.content,
                    createdAt = parseIsoTime(r.created_at)
                )

                val updated = localPosts.value.map { post ->
                    if (post.id == postId) {
                        post.copy(
                            replyCount = post.replyCount + 1,
                            replies = post.replies + newReply
                        )
                    } else {
                        post
                    }
                }
                localPosts.value = updated
                emit(Resource.Success(newReply))
            } else {
                emit(Resource.Error("Could not publish reply on server"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error creating reply"))
        }
    }

    fun getRepliesForPost(postId: String): Flow<Resource<List<NearbyReply>>> = flow {
        emit(Resource.Loading())
        try {
            val response = nearbyApi.getReplies(postId)
            if (response.isSuccessful && response.body() != null) {
                val replies = response.body()!!.replies.map { r ->
                    NearbyReply(
                        id = r.id,
                        postId = r.post_id,
                        userId = r.user_id,
                        authorName = r.author_name,
                        authorAvatarUrl = r.author_avatar_url,
                        authorAvatarColor = Color(0xFFE91E63),
                        content = r.content,
                        createdAt = parseIsoTime(r.created_at)
                    )
                }
                emit(Resource.Success(replies))
            } else {
                emit(Resource.Error("Could not load replies"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error fetching replies"))
        }
    }

    fun reportPost(postId: String) {
        reportedPostIds.add(postId)
        localPosts.value = localPosts.value.filterNot { it.id == postId }
    }

    fun blockUser(userId: String) {
        blockedUserIds.add(userId)
        localPosts.value = localPosts.value.filterNot { it.userId == userId }
    }

    private fun parseIsoTime(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(iso)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

private fun <T> Response<T>.errorText(fallback: String): String {
    val raw = runCatching { errorBody()?.string() }.getOrNull().orEmpty()
    val parsed = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
    return parsed ?: raw.take(180).ifBlank { "$fallback ($code)" }
}
