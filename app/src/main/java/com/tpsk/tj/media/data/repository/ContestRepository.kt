package com.tpsk.tj.media.data.repository

import com.tpsk.tj.media.data.api.ApiContest
import com.tpsk.tj.media.data.api.ApiLeaderboardEntry
import com.tpsk.tj.media.data.api.CastVoteBody
import com.tpsk.tj.media.data.api.ContestApi
import com.tpsk.tj.media.data.api.CreateContestBody
import com.tpsk.tj.media.data.api.RedeemCodeBody
import com.tpsk.tj.media.data.api.SocialApi
import com.tpsk.tj.media.data.api.TopUpBody
import com.tpsk.tj.media.model.Post
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

data class ContestFeed(
    val contest: ApiContest?,
    val leaderboard: List<ApiLeaderboardEntry>,
    val posts: List<Post>
)

@Singleton
class ContestRepository @Inject constructor(
    private val contestApi: ContestApi,
    private val socialApi: SocialApi
) {
    suspend fun list(query: String, sort: String): Result<List<ApiContest>> = runCatching {
        contestApi.list(query.ifBlank { null }, sort).requireBody("Could not load contests").contests
    }

    suspend fun create(title: String, description: String, hashtag: String, limit: Int): Result<ApiContest> = runCatching {
        contestApi.create(
            CreateContestBody(
                title = title.trim(),
                description = description.trim(),
                hashtag = hashtag.trim(),
                participantLimit = limit
            )
        ).requireBody("Could not create contest")
    }

    suspend fun feed(hashtag: String): Result<ContestFeed> = runCatching {
        val clean = hashtag.removePrefix("#").trim()
        val detail = contestApi.get(clean).requireBody("Could not open contest")
        val posts = runCatching {
            socialApi.getPosts(
                page = 1,
                following = false,
                authorId = "",
                feedType = "",
                hashtag = clean
            ).body().orEmpty().map { mapApiPost(it) }
        }.getOrDefault(emptyList())
        val votesByPost = detail.leaderboard.associate { it.postId to it.votes }
        ContestFeed(
            contest = detail.contest,
            leaderboard = detail.leaderboard,
            posts = posts.map { post ->
                post.copy(
                    isContest = detail.contest != null || post.isContest,
                    contestVotes = votesByPost[post.id] ?: post.contestVotes
                )
            }
        )
    }

    suspend fun redeem(hashtag: String, code: String): Result<Unit> = runCatching {
        val response = contestApi.redeem(hashtag.removePrefix("#").trim(), RedeemCodeBody(code.trim()))
        if (!response.isSuccessful) error(response.errorText("Invalid manager code"))
    }

    suspend fun vote(postId: String, votes: Int): Result<VoteResult> = runCatching {
        val body = contestApi.vote(CastVoteBody(postId, votes)).requireBody("Vote failed")
        VoteResult(body.votes, body.points)
    }

    suspend fun points(): Result<Int> = runCatching {
        contestApi.points().requireBody("Could not load points").points
    }

    suspend fun topUp(amount: Int = 10): Result<Int> = runCatching {
        contestApi.topUp(TopUpBody(amount)).requireBody("Could not add points").points
    }

    private fun <T> Response<T>.requireBody(fallback: String): T {
        val body = body()
        if (isSuccessful && body != null) return body
        error(errorText(fallback))
    }

    private fun <T> Response<T>.errorText(fallback: String): String {
        val raw = runCatching { errorBody()?.string() }.getOrNull().orEmpty()
        val parsed = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1)
        return parsed ?: raw.take(180).ifBlank { "$fallback ($code)" }
    }
}

data class VoteResult(val votes: Int, val points: Int)
