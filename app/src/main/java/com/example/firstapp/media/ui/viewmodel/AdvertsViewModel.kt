package com.example.firstapp.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.media.model.Advert
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.media.domain.repository.AdRepository
import com.example.firstapp.vocab.domain.repository.UserRepository

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AdvertsViewModel @Inject constructor(
    private val repository: AdRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    val adverts: StateFlow<Resource<List<Advert>>> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { repository.getAdverts() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    val viewedAdverts: StateFlow<Resource<List<Advert>>> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { repository.getViewedAdverts() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    val points: StateFlow<Int> = userRepository.observeCurrentUser()
        .map { it?.walletBalance ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun fetchAdverts() {
        viewModelScope.launch { 
            repository.clearLocalAdverts()
            refreshTrigger.emit(Unit) 
            userRepository.getCurrentUser().collectLatest { } // Force user refresh
        }
    }

    fun fetchViewedAdverts() {
        viewModelScope.launch { refreshTrigger.emit(Unit) }
    }

    fun markAdvertAsViewed(advertId: Int) {
        viewModelScope.launch {
            repository.markAdvertViewed(advertId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    // Trigger user refresh to get new wallet balance from server
                    userRepository.getCurrentUser().collectLatest { }
                }
            }
        }
    }
}
