package com.example.firstapp.media.ui.viewmodel.shorts

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.data.realtime.PostWebSocketClient
import com.example.firstapp.common.data.realtime.PostSocketListener
import com.example.firstapp.media.model.ShortReply
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.common.util.Resource
import com.example.firstapp.account.domain.repository.AccountRepository
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.firstapp.media.domain.repository.ShortsRepository

@HiltViewModel
class ShortsViewModel @Inject constructor(
    private val repository: ShortsRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _shorts = MutableStateFlow<Resource<List<ShortMedia>>>(Resource.Loading())
    val shorts: StateFlow<Resource<List<ShortMedia>>> = _shorts.asStateFlow()

    private val _userShorts = MutableStateFlow<Resource<List<ShortMedia>>>(Resource.Loading())
    val userShorts: StateFlow<Resource<List<ShortMedia>>> = _userShorts.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _selectedShortId = MutableStateFlow<String?>(null)
    val selectedShortId: StateFlow<String?> = _selectedShortId.asStateFlow()

    private val _selectedShortsUserId = MutableStateFlow<String?>(null)
    val selectedShortsUserId: StateFlow<String?> = _selectedShortsUserId.asStateFlow()

    private val _shortReplies = MutableStateFlow<Resource<List<ShortReply>>>(Resource.Loading())
    val shortReplies: StateFlow<Resource<List<ShortReply>>> = _shortReplies.asStateFlow()

    private var activeShortIdForReplies: String? = null
    private var currentShortsSearchJob: kotlinx.coroutines.Job? = null
    private var postSocketListener: PostSocketListener? = null

    private val shortLikeInFlight = mutableSetOf<String>()
    private val shortSubscribeInFlight = mutableSetOf<String>()
    private val shortLikeLastTapMs = mutableMapOf<String, Long>()
    private val shortSubscribeLastTapMs = mutableMapOf<String, Long>()
    private val shortsTapDebounceMs = 350L

    init {
        fetchShorts()
        connectPostRealtime()
    }

    private fun connectPostRealtime() {
        val listener = object : PostSocketListener {
            override fun onOpen() {}
            override fun onPostsEvent(action: String, postId: String?) {}
            override fun onShortsEvent(action: String, shortId: String?) {
                if (action == "new_short" || action == "reply") {
                    fetchShorts()
                    _selectedShortsUserId.value?.let { getUserShorts(it) }
                    if (action == "reply" && shortId != null && shortId == activeShortIdForReplies) {
                        fetchShortReplies(shortId)
                    }
                }
            }
            override fun onStoriesEvent(action: String, storyId: String?) {}
            override fun onError(error: String) {}
        }
        postSocketListener = listener
        PostWebSocketClient.connect(listener)
    }

    fun fetchShorts() {
        viewModelScope.launch {
            repository.getShorts().collect { _shorts.value = it }
        }
    }

    fun getUserShorts(userId: String) {
        viewModelScope.launch {
            _userShorts.value = Resource.Loading()
            repository.getUserShorts(userId).collect { _userShorts.value = it }
        }
    }

    fun searchShorts(query: String) {
        currentShortsSearchJob?.cancel()
        if (query.isBlank()) { fetchShorts(); return }
        currentShortsSearchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            repository.searchShorts(query).collect { _shorts.value = it }
        }
    }

    fun likeShort(shortId: String) {
        val now = System.currentTimeMillis()
        val lastTap = shortLikeLastTapMs[shortId] ?: 0L
        if (now - lastTap < shortsTapDebounceMs) return
        shortLikeLastTapMs[shortId] = now
        if (shortLikeInFlight.contains(shortId)) return

        val current = _shorts.value.data ?: return
        val previous = current
        val optimistic = current.map { s ->
            if (s.id != shortId) s else {
                val likes = s.likes.toLongOrNull() ?: 0L
                val nextLiked = !s.isLiked
                s.copy(isLiked = nextLiked, likes = (if (nextLiked) likes + 1 else (likes - 1).coerceAtLeast(0)).toString())
            }
        }
        _shorts.value = Resource.Success(optimistic)
        shortLikeInFlight.add(shortId)

        viewModelScope.launch {
            repository.likeShort(shortId).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    val body = resource.data
                    val synced = ( _shorts.value.data ?: optimistic ).map { s ->
                        if (s.id == shortId) s.copy(isLiked = body.is_liked, likes = body.likes.toString()) else s
                    }
                    _shorts.value = Resource.Success(synced)
                } else if (resource is Resource.Error) {
                    _shorts.value = Resource.Success(previous)
                }
            }
            shortLikeInFlight.remove(shortId)
        }
    }

    fun subscribeToShortCreator(shortId: String) {
        val now = System.currentTimeMillis()
        val lastTap = shortSubscribeLastTapMs[shortId] ?: 0L
        if (now - lastTap < shortsTapDebounceMs) return
        shortSubscribeLastTapMs[shortId] = now
        if (shortSubscribeInFlight.contains(shortId)) return

        val current = _shorts.value.data ?: return
        val previous = current
        val optimistic = current.map { s -> if (s.id == shortId) s.copy(isSubscribed = !s.isSubscribed) else s }
        _shorts.value = Resource.Success(optimistic)
        shortSubscribeInFlight.add(shortId)

        viewModelScope.launch {
            repository.subscribeToShortCreator(shortId).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    val body = resource.data
                    val synced = ( _shorts.value.data ?: optimistic ).map { s ->
                        if (s.id == shortId) s.copy(isSubscribed = body.is_subscribed) else s
                    }
                    _shorts.value = Resource.Success(synced)
                } else if (resource is Resource.Error) {
                    _shorts.value = Resource.Success(previous)
                }
            }
            shortSubscribeInFlight.remove(shortId)
        }
    }

    fun repostShort(shortId: String) { viewModelScope.launch { repository.repostShort(shortId).collect { if (it is Resource.Success && it.data == true) fetchShorts() } } }

    fun bookmarkShort(shortId: String) {
        val current = _shorts.value.data ?: return
        val optimistic = current.map { 
            if (it.id == shortId) it.copy(isBookmarked = !it.isBookmarked) else it 
        }
        _shorts.value = Resource.Success(optimistic)
        viewModelScope.launch { repository.bookmarkShort(shortId).collect { } }
    }

    fun trackShortShare(shortId: String) {
        val current = _shorts.value.data ?: return
        val optimistic = current.map { 
            if (it.id == shortId) {
                val sCount = it.shares.toLongOrNull() ?: 0L
                it.copy(shares = (sCount + 1).toString())
            } else it
        }
        _shorts.value = Resource.Success(optimistic)
        viewModelScope.launch { repository.trackShortShare(shortId).collect { } }
    }

    fun fetchShortReplies(shortId: String) {
        activeShortIdForReplies = shortId
        viewModelScope.launch {
            _shortReplies.value = Resource.Loading()
            repository.getShortReplies(shortId).collect { _shortReplies.value = it }
        }
    }

    fun likeShortReply(replyId: String) {
        viewModelScope.launch {
            repository.likeShortReply(replyId).collect { resource ->
                if (resource is Resource.Success && resource.data == true) {
                    activeShortIdForReplies?.let { fetchShortReplies(it) }
                }
            }
        }
    }

    fun replyToShort(shortId: String, content: String, parentReplyId: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.replyToShort(shortId, content, parentReplyId).collect { if (it is Resource.Success && it.data == true) { fetchShorts(); fetchShortReplies(shortId) } }
        }
    }

    fun clearActiveShortReplies() { activeShortIdForReplies = null }

    fun setSelectedShortId(id: String?) {
        _selectedShortId.value = id
    }

    fun setSelectedShortsUserId(userId: String?) {
        _selectedShortsUserId.value = userId
        if (userId != null) {
            getUserShorts(userId)
        } else {
            fetchShorts()
        }
    }

    fun uploadShort(videoFile: File, description: String, thumbnailFile: File?) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                var videoUrl = ""
                var thumbnailUrl = ""

                // 1. Upload Video
                val videoRes = accountRepository.uploadFile(videoFile, "shorts/videos").first()
                if (videoRes is Resource.Success && videoRes.data != null) {
                    videoUrl = videoRes.data.url
                } else {
                    _shorts.value = Resource.Error((videoRes as? Resource.Error)?.message ?: "Video upload failed")
                    _isUploading.value = false
                    return@launch
                }

                // 2. Upload Thumbnail if provided
                if (thumbnailFile != null) {
                    val thumbRes = accountRepository.uploadFile(thumbnailFile, "shorts/thumbnails").first()
                    if (thumbRes is Resource.Success && thumbRes.data != null) {
                        thumbnailUrl = thumbRes.data.url
                    }
                }

                // 3. Create Short entry
                repository.createShort(description, videoUrl, thumbnailUrl).collect { res ->
                    if (res is Resource.Success) {
                        fetchShorts()
                        _selectedShortsUserId.value?.let { getUserShorts(it) }
                    } else if (res is Resource.Error) {
                        _shorts.value = Resource.Error(res.message ?: "Failed to create short")
                    }
                }
            } catch (e: Exception) {
                _shorts.value = Resource.Error(e.message ?: "Unknown upload error")
            } finally {
                _isUploading.value = false
            }
        }
    }

    override fun onCleared() {
        postSocketListener?.let { PostWebSocketClient.disconnect(it) }
        super.onCleared()
    }
}
