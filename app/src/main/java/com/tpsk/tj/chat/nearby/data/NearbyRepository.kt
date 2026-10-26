@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.tpsk.tj.chat.nearby.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.tpsk.tj.chat.nearby.data.api.ApiChatRequest
import com.tpsk.tj.chat.nearby.data.api.ApiNearbyPost
import com.tpsk.tj.chat.nearby.data.api.CreateChatRequestBody
import com.tpsk.tj.chat.nearby.data.api.CreateNearbyPostRequest
import com.tpsk.tj.chat.nearby.data.api.CreateNearbyReplyRequest
import com.tpsk.tj.chat.nearby.data.api.NearbyApi
import com.tpsk.tj.chat.nearby.data.api.ResolveNearbyPostRequest
import com.tpsk.tj.chat.nearby.data.api.RespondChatRequestBody
import com.tpsk.tj.chat.nearby.data.api.UpdateNearbyPostRequest
import com.tpsk.tj.chat.nearby.model.AreaPlace
import com.tpsk.tj.chat.nearby.model.NearbyChatRequest
import com.tpsk.tj.chat.nearby.model.NearbyPost
import com.tpsk.tj.chat.nearby.model.NearbyReply
import com.tpsk.tj.common.util.MediaUtils
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.data.api.MediaApi
import com.tpsk.tj.media.util.MediaProcessor
import retrofit2.Response
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class NearbyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nearbyApi: NearbyApi,
    private val mediaApi: MediaApi
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nearby_location_prefs", Context.MODE_PRIVATE)
    private val reportedPostIds = mutableSetOf<String>()
    private val blockedUserIds = mutableSetOf<String>()

    private val localPosts = MutableStateFlow<List<NearbyPost>>(emptyList())
    private val chatRequests = MutableStateFlow<List<NearbyChatRequest>>(emptyList())

    fun observePosts(): StateFlow<List<NearbyPost>> = localPosts.asStateFlow()

    fun findPost(id: String): NearbyPost? = localPosts.value.find { it.id == id }

    suspend fun fetchPost(id: String): NearbyPost? {
        val response = runCatching { nearbyApi.getPost(id) }.getOrNull() ?: return null
        if (!response.isSuccessful) return null
        val api = response.body() ?: return null
        return NearbyPost(
            id = api.id,
            userId = api.user_id,
            authorName = api.author_name,
            authorAvatarUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(api.author_avatar_url),
            content = api.content,
            coarseAreaName = api.area_name.ifBlank { "Nearby" },
            latitude = api.latitude,
            longitude = api.longitude,
            distanceKm = api.distance_km,
            likeCount = api.like_count,
            replyCount = api.reply_count,
            isLiked = api.is_liked,
            isResolved = api.is_resolved,
            imageUrls = resolvedImageUrls(api),
            createdAt = parseIsoTime(api.created_at)
        )
    }

    suspend fun fetchPostImages(id: String): List<String> {
        val cached = findPost(id)?.imageUrls.orEmpty()
        if (cached.isNotEmpty()) return cached
        val response = runCatching { nearbyApi.getPost(id) }.getOrNull() ?: return emptyList()
        if (!response.isSuccessful) return emptyList()
        return response.body()?.let { resolvedImageUrls(it) }.orEmpty()
    }

    fun saveCoarseLocation(lat: Double, lng: Double, areaName: String) {
        val roundedLat = (lat * 1000).roundToInt() / 1000.0
        val roundedLng = (lng * 1000).roundToInt() / 1000.0
        prefs.edit()
            .putFloat("coarse_lat", roundedLat.toFloat())
            .putFloat("coarse_lng", roundedLng.toFloat())
            .putString("coarse_area_name", areaName)
            .putLong("cached_at_ms", System.currentTimeMillis())
            .apply()
    }

    fun getCachedCoarseAreaName(): String {
        return prefs.getString("coarse_area_name", null)?.takeIf { it.isNotBlank() } ?: "Nearby"
    }

    fun getCachedCoarseLat(): Double {
        return prefs.getFloat("coarse_lat", 0f).toDouble()
    }

    fun getCachedCoarseLng(): Double {
        return prefs.getFloat("coarse_lng", 0f).toDouble()
    }

    fun hasRealCachedLocation(): Boolean {
        if (!prefs.contains("cached_at_ms")) return false
        val lat = getCachedCoarseLat()
        val lng = getCachedCoarseLng()
        if (lat == 0.0 || lng == 0.0) return false
        val sentinel = kotlin.math.abs(lat - (-26.19)) < 0.001 && kotlin.math.abs(lng - 28.03) < 0.001
        val name = prefs.getString("coarse_area_name", null)
        return !(sentinel && (name.isNullOrBlank() || name.equals("Braamfontein", ignoreCase = true)))
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
                        authorAvatarUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(api.author_avatar_url),
                        authorAvatarColor = Color(0xFF00B4D8),
                        content = api.content,
                        coarseAreaName = api.area_name,
                        latitude = api.latitude,
                        longitude = api.longitude,
                        distanceKm = api.distance_km,
                        likeCount = api.like_count,
                        replyCount = maxOf(api.reply_count, api.replies?.size ?: 0),
                        isLiked = api.is_liked,
                        isResolved = api.is_resolved,
                        imageUrls = resolvedImageUrls(api),
                        createdAt = parseIsoTime(api.created_at),
                        replies = api.replies?.map { r ->
                            NearbyReply(
                                id = r.id,
                                postId = r.post_id,
                                userId = r.user_id,
                                authorName = r.author_name,
                                authorAvatarUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(r.author_avatar_url),
                                authorAvatarColor = Color(0xFFE91E63),
                                content = r.content,
                                createdAt = parseIsoTime(r.created_at)
                            )
                        } ?: emptyList()
                    )
                }.filter { post ->
                    !reportedPostIds.contains(post.id) && !blockedUserIds.contains(post.userId)
                }
                // Merge with any local user-created posts not yet returned by server
                val previousImages = localPosts.value.associate { it.id to it.imageUrls }
                val withImages = mapped.map { post ->
                    if (post.imageUrls.isNotEmpty()) post
                    else post.copy(imageUrls = previousImages[post.id].orEmpty().mapNotNull { MediaUtils.formatUrl(it) })
                }
                val existingLocalOnly = localPosts.value.filter { local ->
                    withImages.none { it.id == local.id } && !local.id.startsWith("local_")
                }
                val merged = existingLocalOnly + withImages
                localPosts.value = merged
                emit(Resource.Success(merged))
            } else {
                emit(Resource.Error(response.errorText("Unable to fetch nearby posts"), localPosts.value))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error fetching nearby posts", localPosts.value))
        }
    }

    suspend fun pingArea(areaName: String, townName: String, lat: Double, lng: Double): Boolean {
        if (areaName.isBlank() || lat == 0.0 || lng == 0.0) return false
        return withContext(Dispatchers.IO) {
            runCatching {
                nearbyApi.ping(
                    com.tpsk.tj.chat.nearby.data.api.NearbyPingRequest(
                        latitude = lat,
                        longitude = lng,
                        area_name = areaName,
                        town_name = townName.ifBlank { areaName }
                    )
                ).isSuccessful
            }.getOrDefault(false)
        }
    }

    suspend fun searchPostedAreas(query: String, lat: Double, lng: Double): List<AreaPlace> {
        val response = if (query.isBlank()) {
            nearbyApi.suggestAreas(
                lat = lat.takeIf { it != 0.0 },
                lng = lng.takeIf { it != 0.0 }
            )
        } else {
            nearbyApi.searchAreas(query.trim())
        }
        if (!response.isSuccessful) {
            if (response.code() == 404) return areasFromKnownPosts(query)
            error("Could not load posted areas (${response.code()})")
        }
        val remote = response.body()?.areas.orEmpty().mapNotNull { area ->
            val label = area.display_name.ifBlank {
                if (area.town.isBlank() || area.town.equals(area.name, true)) area.name else "${area.name}, ${area.town}"
            }.trim()
            if (label.isBlank() || label.equals("Nearby", true) || label.equals("Braamfontein", true)) return@mapNotNull null
            if (area.latitude == 0.0 || area.longitude == 0.0) return@mapNotNull null
            AreaPlace(label, area.latitude, area.longitude, area.post_count, area.view_count)
        }
        return remote.ifEmpty { areasFromKnownPosts(query) }
    }

    fun createPost(
        content: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String? = null,
        areaName: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        imageUrls: List<String> = emptyList()
    ): Flow<Resource<NearbyPost>> = flow {
        val trimmed = content.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isBlank() && imageUrls.isEmpty()) {
            emit(Resource.Error("Add text or a photo before posting"))
            return@flow
        }
        if (trimmed.length > 500) {
            emit(Resource.Error("Post exceeds 500 characters limit"))
            return@flow
        }

        emit(Resource.Loading())
        try {
            val uploaded = uploadLocalMedia(imageUrls)
            val postLat = latitude ?: getCachedCoarseLat()
            val postLng = longitude ?: getCachedCoarseLng()
            val targetArea = areaName?.takeIf { it.isNotBlank() } ?: getCachedCoarseAreaName()
            val req = CreateNearbyPostRequest(
                content = trimmed.ifBlank { "Photo" },
                area_name = targetArea,
                latitude = postLat,
                longitude = postLng,
                image_urls = uploaded
            )
            val response = nearbyApi.createPost(req)
            if (response.isSuccessful && response.body() != null) {
                val api = response.body()!!
                val savedImages = resolvedImageUrls(api).ifEmpty { uploaded.mapNotNull { MediaUtils.formatUrl(it) } }
                val newPost = NearbyPost(
                    id = api.id,
                    userId = api.user_id,
                    authorName = if (api.author_name.isNotBlank() && api.author_name != "User") api.author_name else authorName,
                    authorAvatarUrl = MediaUtils.formatUrl(api.author_avatar_url) ?: authorAvatarUrl,
                    authorAvatarColor = Color(0xFF00B4D8),
                    content = api.content.ifBlank { trimmed },
                    coarseAreaName = api.area_name.ifBlank { targetArea },
                    latitude = api.latitude,
                    longitude = api.longitude,
                    distanceKm = 0.5,
                    likeCount = api.like_count,
                    replyCount = api.reply_count,
                    isLiked = false,
                    imageUrls = savedImages,
                    createdAt = parseIsoTime(api.created_at)
                )
                localPosts.value = listOf(newPost) + localPosts.value.filter { it.id != newPost.id }
                emit(Resource.Success(newPost))
            } else {
                emit(Resource.Error(response.errorText("Could not publish nearby post")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Could not publish nearby post"))
        }
    }

    fun getMyPosts(): Flow<Resource<List<NearbyPost>>> = flow {
        emit(Resource.Loading())
        try {
            val response = nearbyApi.getMyPosts()
            if (response.isSuccessful && response.body() != null) {
                val mapped = response.body()!!.posts.map { api ->
                    NearbyPost(
                        id = api.id,
                        userId = api.user_id,
                        authorName = api.author_name,
                        authorAvatarUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(api.author_avatar_url),
                        content = api.content,
                        coarseAreaName = api.area_name,
                        latitude = api.latitude,
                        longitude = api.longitude,
                        likeCount = api.like_count,
                        replyCount = api.reply_count,
                        isResolved = api.is_resolved,
                        imageUrls = resolvedImageUrls(api),
                        createdAt = parseIsoTime(api.created_at)
                    )
                }
                emit(Resource.Success(mapped))
            } else {
                emit(Resource.Error(response.errorText("Could not load your nearby posts")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading your posts"))
        }
    }

    fun deletePost(postId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = nearbyApi.deletePost(postId)
            if (response.isSuccessful) {
                localPosts.value = localPosts.value.filterNot { it.id == postId }
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.errorText("Could not delete post")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error deleting post"))
        }
    }

    fun markPostResolved(postId: String, isResolved: Boolean): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val response = nearbyApi.resolvePost(postId, ResolveNearbyPostRequest(isResolved))
            if (response.isSuccessful) {
                localPosts.value = localPosts.value.map { post ->
                    if (post.id == postId) post.copy(isResolved = isResolved) else post
                }
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.errorText("Could not update post")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error updating post"))
        }
    }

    fun editPost(postId: String, newContent: String, newArea: String): Flow<Resource<Unit>> = flow {
        val trimmed = newContent.trim()
        if (trimmed.isBlank()) {
            emit(Resource.Error("Post content cannot be empty"))
            return@flow
        }
        emit(Resource.Loading())
        try {
            val response = nearbyApi.updatePost(postId, UpdateNearbyPostRequest(trimmed, newArea.ifBlank { "Nearby" }))
            if (response.isSuccessful) {
                localPosts.value = localPosts.value.map { post ->
                    if (post.id == postId) post.copy(content = trimmed, coarseAreaName = newArea.ifBlank { post.coarseAreaName }) else post
                }
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.errorText("Could not edit post")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error editing post"))
        }
    }

    fun sendChatRequest(postId: String, inquiryMessage: String): Flow<Resource<NearbyChatRequest>> = flow {
        val trimmed = inquiryMessage.trim()
        if (trimmed.isBlank()) {
            emit(Resource.Error("Message cannot be empty"))
            return@flow
        }
        emit(Resource.Loading())
        try {
            val response = nearbyApi.createChatRequest(CreateChatRequestBody(postId, trimmed))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.toModel()))
            } else {
                emit(Resource.Error(response.errorText("Could not send chat request")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error sending request"))
        }
    }

    fun refreshChatRequests(): Flow<Resource<List<NearbyChatRequest>>> = flow {
        emit(Resource.Loading())
        try {
            val response = nearbyApi.getChatRequests()
            if (response.isSuccessful && response.body() != null) {
                val mapped = response.body()!!.requests.map { it.toModel() }
                chatRequests.value = mapped
                emit(Resource.Success(mapped))
            } else {
                emit(Resource.Error(response.errorText("Could not load chat requests")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error loading requests"))
        }
    }

    fun getChatRequests(): Flow<List<NearbyChatRequest>> = chatRequests

    fun respondChatRequest(requestId: String, accept: Boolean): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = nearbyApi.respondChatRequest(requestId, RespondChatRequestBody(accept))
            if (response.isSuccessful) {
                val senderId = response.body()?.get("sender_id").orEmpty()
                chatRequests.value = chatRequests.value.filterNot { it.id == requestId }
                emit(Resource.Success(senderId))
            } else {
                emit(Resource.Error(response.errorText("Could not respond to request")))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error responding to request"))
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
                    authorAvatarUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(r.author_avatar_url) ?: authorAvatarUrl,
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
                emit(Resource.Error(response.errorText("Could not publish reply")))
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
                        authorAvatarUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(r.author_avatar_url),
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

    private fun resolvedImageUrls(api: ApiNearbyPost): List<String> {
        return api.image_urls.mapNotNull { MediaUtils.formatUrl(it) }
    }

    private fun areasFromKnownPosts(query: String): List<AreaPlace> {
        val needle = query.trim()
        return localPosts.value.mapNotNull { post ->
            val label = post.coarseAreaName.trim()
            if (label.isBlank() || label.equals("Nearby", true) || label.equals("Braamfontein", true)) return@mapNotNull null
            if (post.latitude == 0.0 || post.longitude == 0.0) return@mapNotNull null
            if (needle.isNotEmpty() && !label.contains(needle, true)) return@mapNotNull null
            AreaPlace(label, post.latitude, post.longitude, 1)
        }.distinctBy { it.displayName.lowercase() }
    }

    private suspend fun uploadLocalMedia(uris: List<String>): List<String> = withContext(Dispatchers.IO) {
        uris.take(4).map { raw ->
            if (raw.startsWith("http://") || raw.startsWith("https://") || raw.startsWith("uploads/") || raw.startsWith("/uploads/")) {
                raw
            } else {
                val uri = Uri.parse(raw)
                val mime = context.contentResolver.getType(uri).orEmpty()
                val file = if (mime.startsWith("video/")) {
                    com.tpsk.tj.common.util.VideoTransformerManager.exportForUpload(
                        context, uri, com.tpsk.tj.common.util.VideoUploadProfile.GENERAL
                    )
                } else {
                    MediaProcessor.processAndStripExif(context, uri)
                }
                try {
                    val mimeType = if (mime.startsWith("video/")) mime else "image/jpeg"
                    val body = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
                    val folder = "nearby".toRequestBody("text/plain".toMediaTypeOrNull())
                    val upload = mediaApi.uploadMedia(body, folder)
                    val url = upload.body()?.url
                    if (!upload.isSuccessful || url.isNullOrBlank()) {
                        error("Photo upload failed (${upload.code()})")
                    }
                    url
                } finally {
                    if (file.exists()) file.delete()
                }
            }
        }
    }

    private fun copyUriToCache(uri: Uri, extension: String): File {
        val out = File(context.cacheDir, "nearby_${UUID.randomUUID()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not open the selected photo")
        return out
    }
}

private fun ApiChatRequest.toModel() = NearbyChatRequest(
    id = id,
    senderId = sender_id,
    senderName = sender_name.ifBlank { "Someone nearby" },
    senderAvatarUrl = com.tpsk.tj.common.util.MediaUtils.formatUrl(sender_avatar_url),
    recipientId = recipient_id,
    recipientName = recipient_name,
    postReferenceSnippet = post_reference_snippet,
    postId = post_id,
    inquiryMessage = inquiry_message,
    createdAt = parseIsoTime(created_at)
)

private fun parseIsoTime(iso: String?): Long {
    if (iso.isNullOrBlank()) return System.currentTimeMillis()
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.parse(iso)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}

private fun <T> Response<T>.errorText(fallback: String): String {
    val raw = runCatching { errorBody()?.string() }.getOrNull().orEmpty()
    val parsed = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
    return parsed ?: raw.take(180).ifBlank { "$fallback (${code()})" }
}
