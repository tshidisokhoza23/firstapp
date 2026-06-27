package com.example.firstapp.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.search.model.SearchResult
import com.example.firstapp.common.model.*
import com.example.firstapp.media.model.*
import com.example.firstapp.music.model.Song
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.media.domain.repository.AdRepository
import com.example.firstapp.media.domain.repository.ChannelRepository
import com.example.firstapp.music.domain.repository.MusicRepository
import com.example.firstapp.search.domain.repository.SearchRepository

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val adRepository: AdRepository,
    private val channelRepository: ChannelRepository,
    private val musicRepository: MusicRepository,
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _adverts = MutableStateFlow<Resource<List<Advert>>>(Resource.Loading())
    val adverts: StateFlow<Resource<List<Advert>>> = _adverts.asStateFlow()

    private val _channels = MutableStateFlow<Resource<List<Channel>>>(Resource.Loading())
    val channels: StateFlow<Resource<List<Channel>>> = _channels.asStateFlow()

    private val _songs = MutableStateFlow<Resource<List<Song>>>(Resource.Loading())
    val songs: StateFlow<Resource<List<Song>>> = _songs.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResult?>(null)
    val searchResults: StateFlow<SearchResult?> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var currentSearchJob: kotlinx.coroutines.Job? = null

    init {
        fetchAdverts()
        fetchChannels()
        fetchSongs()
    }

    fun fetchAdverts() {
        viewModelScope.launch {
            adRepository.getAdverts().collect { _adverts.value = it }
        }
    }

    fun markAdvertAsViewed(advertId: Int) {
        val current = _adverts.value
        if (current is Resource.Success) {
            val updated = current.data?.map { if (it.id == advertId) it.copy(isViewed = true) else it }
            _adverts.value = Resource.Success(updated ?: emptyList())
            viewModelScope.launch {
                adRepository.markAdvertViewed(advertId).collect {}
            }
        }
    }

    fun fetchChannels() {
        viewModelScope.launch {
            channelRepository.getChannels().collect { _channels.value = it }
        }
    }

    fun subscribeToChannel(channelId: Int) {
        viewModelScope.launch { channelRepository.subscribeToChannel(channelId).collect { if (it is Resource.Success) fetchChannels() } }
    }

    fun muteChannel(channelId: Int, isMuted: Boolean) {
        viewModelScope.launch { channelRepository.muteChannel(channelId, isMuted).collect { if (it is Resource.Success) fetchChannels() } }
    }

    fun fetchSongs() {
        viewModelScope.launch {
            musicRepository.getSongs().collect { _songs.value = it }
        }
    }

    fun incrementSongPlay(songId: Int) {
        viewModelScope.launch { musicRepository.incrementSongPlay(songId).collect { } }
    }

    fun search(query: String) {
        currentSearchJob?.cancel()
        if (query.isBlank()) { _searchResults.value = null; _isSearching.value = false; return }
        currentSearchJob = viewModelScope.launch {
            delay(300)
            _isSearching.value = true
            searchRepository.search(query).collect { result ->
                if (result is Resource.Success) _searchResults.value = result.data
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        currentSearchJob?.cancel()
        _searchResults.value = null
    }
}
