package com.example.firstapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.data.repository.AppRepository
import com.example.firstapp.data.repository.AppRepositoryImpl
import com.example.firstapp.model.*
import com.example.firstapp.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: AppRepository = AppRepositoryImpl()
) : ViewModel() {

    private val _chats = MutableStateFlow<Resource<List<ChatMessage>>>(Resource.Loading())
    val chats: StateFlow<Resource<List<ChatMessage>>> = _chats.asStateFlow()

    private val _jobs = MutableStateFlow<Resource<List<Job>>>(Resource.Loading())
    val jobs: StateFlow<Resource<List<Job>>> = _jobs.asStateFlow()

    private val _adverts = MutableStateFlow<Resource<List<Advert>>>(Resource.Loading())
    val adverts: StateFlow<Resource<List<Advert>>> = _adverts.asStateFlow()

    private val _channels = MutableStateFlow<Resource<List<Channel>>>(Resource.Loading())
    val channels: StateFlow<Resource<List<Channel>>> = _channels.asStateFlow()

    private val _shorts = MutableStateFlow<Resource<List<com.example.firstapp.model.Short>>>(Resource.Loading())
    val shorts: StateFlow<Resource<List<com.example.firstapp.model.Short>>> = _shorts.asStateFlow()

    private val _songs = MutableStateFlow<Resource<List<Song>>>(Resource.Loading())
    val songs: StateFlow<Resource<List<Song>>> = _songs.asStateFlow()

    private val _posts = MutableStateFlow<Resource<List<Post>>>(Resource.Loading())
    val posts: StateFlow<Resource<List<Post>>> = _posts.asStateFlow()

    init {
        fetchChats()
        fetchJobs()
        fetchAdverts()
        fetchChannels()
        fetchShorts()
        fetchSongs()
        fetchPosts()
    }

    fun fetchPosts() {
        viewModelScope.launch {
            repository.getPosts().collect { _posts.value = it }
        }
    }

    fun fetchSongs() {
        viewModelScope.launch {
            repository.getSongs().collect { _songs.value = it }
        }
    }

    fun fetchShorts() {
        viewModelScope.launch {
            repository.getShorts().collect { _shorts.value = it }
        }
    }

    fun fetchChats() {
        viewModelScope.launch {
            repository.getChats().collect { _chats.value = it }
        }
    }

    fun fetchJobs() {
        viewModelScope.launch {
            repository.getJobs().collect { _jobs.value = it }
        }
    }

    fun fetchAdverts() {
        viewModelScope.launch {
            repository.getAdverts().collect { _adverts.value = it }
        }
    }

    fun fetchChannels() {
        viewModelScope.launch {
            repository.getChannels().collect { _channels.value = it }
        }
    }
}
