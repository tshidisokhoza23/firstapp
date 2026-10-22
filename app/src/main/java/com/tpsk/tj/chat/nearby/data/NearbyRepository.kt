package com.tpsk.tj.chat.nearby.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.tpsk.tj.chat.nearby.model.NearbyPost
import com.tpsk.tj.chat.nearby.model.NearbyReply
import com.tpsk.tj.common.util.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class NearbyRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nearby_location_prefs", Context.MODE_PRIVATE)
    private val reportedPostIds = mutableSetOf<String>()
    private val blockedUserIds = mutableSetOf<String>()
    private var lastPostTimestampMs: Long = 0L

    private val localPosts = MutableStateFlow<List<NearbyPost>>(createInitialSamplePosts())

    fun saveCoarseLocation(lat: Double, lng: Double, areaName: String) {
        // Privacy rule: Round coordinates to 2 decimal places on device
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
            val areaName = selectedCity ?: getCachedCoarseAreaName()
            val filtered = localPosts.value.filter { post ->
                !reportedPostIds.contains(post.id) &&
                        !blockedUserIds.contains(post.userId) &&
                        post.distanceKm <= radiusKm
            }
            emit(Resource.Success(filtered))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unable to fetch nearby posts"))
        }
    }

    fun createPost(
        content: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String? = null
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

        val now = System.currentTimeMillis()
        if (now - lastPostTimestampMs < 2 * 60 * 1000L) {
            val remainingSec = ((2 * 60 * 1000L - (now - lastPostTimestampMs)) / 1000L).coerceAtLeast(1)
            emit(Resource.Error("Rate limit: Please wait $remainingSec seconds before posting again"))
            return@flow
        }

        emit(Resource.Loading())
        val areaName = getCachedCoarseAreaName()
        val newPost = NearbyPost(
            id = "nearby_" + UUID.randomUUID().toString().take(8),
            userId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            authorAvatarColor = Color(0xFF00B4D8),
            content = trimmed,
            coarseAreaName = areaName,
            distanceKm = 0.5,
            likeCount = 0,
            replyCount = 0,
            isLiked = false,
            createdAt = now
        )

        lastPostTimestampMs = now
        localPosts.value = listOf(newPost) + localPosts.value
        emit(Resource.Success(newPost))
    }

    fun likePost(postId: String): Flow<Resource<NearbyPost>> = flow {
        val current = localPosts.value
        val updated = current.map { post ->
            if (post.id == postId) {
                val newLiked = !post.isLiked
                val newLikes = if (newLiked) post.likeCount + 1 else (post.likeCount - 1).coerceAtLeast(0)
                post.copy(isLiked = newLiked, likeCount = newLikes)
            } else {
                post
            }
        }
        localPosts.value = updated
        updated.find { it.id == postId }?.let { emit(Resource.Success(it)) }
            ?: emit(Resource.Error("Post not found"))
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

        val newReply = NearbyReply(
            id = "reply_" + UUID.randomUUID().toString().take(8),
            postId = postId,
            userId = authorId,
            authorName = authorName,
            authorAvatarUrl = authorAvatarUrl,
            authorAvatarColor = Color(0xFFE91E63),
            content = trimmed,
            createdAt = System.currentTimeMillis()
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
    }

    fun reportPost(postId: String) {
        reportedPostIds.add(postId)
        localPosts.value = localPosts.value.filterNot { it.id == postId }
    }

    fun blockUser(userId: String) {
        blockedUserIds.add(userId)
        localPosts.value = localPosts.value.filterNot { it.userId == userId }
    }

    private fun createInitialSamplePosts(): List<NearbyPost> {
        val now = System.currentTimeMillis()
        return listOf(
            NearbyPost(
                id = "nearby_sample_1",
                userId = "user_sizwe",
                authorName = "Sizwe M.",
                authorAvatarColor = Color(0xFF00B4D8),
                content = "Anyone know why the power is out near the main campus area?",
                coarseAreaName = "Braamfontein",
                distanceKm = 1.5,
                likeCount = 15,
                replyCount = 42,
                isLiked = false,
                createdAt = now - (5 * 60 * 1000L),
                recentCommenterAvatars = listOf("avatar_1", "avatar_2", "avatar_3"),
                replies = listOf(
                    NearbyReply(
                        id = "r1",
                        postId = "nearby_sample_1",
                        userId = "user_thabo",
                        authorName = "Thabo K.",
                        content = "City Power said there is a cable fault at the local substation. Expected resolution in 2 hours.",
                        createdAt = now - (3 * 60 * 1000L)
                    )
                )
            ),
            NearbyPost(
                id = "nearby_sample_2",
                userId = "user_kgotso",
                authorName = "Kgotso",
                authorAvatarColor = Color(0xFFE91E63),
                content = "Selling my textbook for second year econ. Perfect condition, DM me!",
                coarseAreaName = "Braamfontein",
                distanceKm = 2.1,
                likeCount = 4,
                replyCount = 8,
                isLiked = false,
                createdAt = now - (25 * 60 * 1000L)
            ),
            NearbyPost(
                id = "nearby_sample_3",
                userId = "user_lerato",
                authorName = "Lerato P.",
                authorAvatarColor = Color(0xFF4CAF50),
                content = "Study group meeting at the library second floor study room at 4 PM for CS201. Everyone welcome!",
                coarseAreaName = "Milpark",
                distanceKm = 3.8,
                likeCount = 22,
                replyCount = 12,
                isLiked = false,
                createdAt = now - (2 * 3600 * 1000L)
            ),
            NearbyPost(
                id = "nearby_sample_4",
                userId = "user_ayanda",
                authorName = "Ayanda B.",
                authorAvatarColor = Color(0xFFFF9800),
                content = "Lost silver keys with a blue keychain near the coffee shop on Jorissen St. Please let me know if found!",
                coarseAreaName = "Braamfontein",
                distanceKm = 0.8,
                likeCount = 18,
                replyCount = 9,
                isLiked = false,
                createdAt = now - (4 * 3600 * 1000L)
            )
        )
    }
}
