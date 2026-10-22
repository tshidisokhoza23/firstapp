package com.tpsk.tj.ui.screens.contest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.media.data.api.ApiContest
import com.tpsk.tj.media.data.api.ApiLeaderboardEntry
import com.tpsk.tj.media.data.repository.ContestRepository
import com.tpsk.tj.media.model.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContestListState(
    val isLoading: Boolean = false,
    val contests: List<ApiContest> = emptyList(),
    val error: String? = null
)

data class ContestFeedState(
    val isLoading: Boolean = true,
    val contest: ApiContest? = null,
    val leaderboard: List<ApiLeaderboardEntry> = emptyList(),
    val posts: List<Post> = emptyList(),
    val error: String? = null,
    val points: Int = 0
)

@HiltViewModel
class ContestViewModel @Inject constructor(
    private val repository: ContestRepository
) : ViewModel() {
    private val _listState = MutableStateFlow(ContestListState())
    val listState: StateFlow<ContestListState> = _listState.asStateFlow()

    private val _feedState = MutableStateFlow(ContestFeedState())
    val feedState: StateFlow<ContestFeedState> = _feedState.asStateFlow()

    private val _points = MutableStateFlow(20)
    val points: StateFlow<Int> = _points.asStateFlow()

    private val _activeHashtags = MutableStateFlow<List<String>>(emptyList())
    val activeHashtags: StateFlow<List<String>> = _activeHashtags.asStateFlow()

    fun loadContests(query: String, sort: String) {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true, error = null) }
            repository.list(query, sort)
                .onSuccess { rows ->
                    _listState.update { it.copy(isLoading = false, contests = rows) }
                    _activeHashtags.value = rows.map { row -> row.hashtag }
                }
                .onFailure { err ->
                    _listState.update { it.copy(isLoading = false, error = err.message) }
                }
        }
    }

    fun loadActiveHashtags() {
        viewModelScope.launch {
            repository.list("", "new").onSuccess { rows ->
                _activeHashtags.value = rows.map { it.hashtag }
            }
        }
    }

    fun loadFeed(hashtag: String) {
        viewModelScope.launch {
            _feedState.update { it.copy(isLoading = true, error = null) }
            repository.feed(hashtag)
                .onSuccess { feed ->
                    _feedState.update {
                        it.copy(
                            isLoading = false,
                            contest = feed.contest,
                            leaderboard = feed.leaderboard,
                            posts = feed.posts
                        )
                    }
                }
                .onFailure { err ->
                    _feedState.update { it.copy(isLoading = false, error = err.message ?: "Could not load contest") }
                }
            repository.points().onSuccess { pts ->
                _points.value = pts
                _feedState.update { it.copy(points = pts) }
            }
        }
    }

    fun createContest(
        title: String,
        description: String,
        hashtag: String,
        limit: Int,
        onSuccess: (ApiContest) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.create(title, description, hashtag, limit)
                .onSuccess(onSuccess)
                .onFailure { onError(it.message ?: "Could not create contest") }
        }
    }

    suspend fun redeem(hashtag: String, code: String): Result<Unit> = repository.redeem(hashtag, code)

    fun castVote(postId: String, votes: Int, onResult: (Result<Pair<Int, Int>>) -> Unit) {
        viewModelScope.launch {
            val result = repository.vote(postId, votes).map { it.votes to it.points }
            result.onSuccess { (_, points) -> _points.value = points }
            onResult(result)
        }
    }

    fun topUp(onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = repository.topUp()
            result.onSuccess { _points.value = it }
            onResult(result)
        }
    }

    fun refreshPoints() {
        viewModelScope.launch {
            repository.points().onSuccess { _points.value = it }
        }
    }
}
