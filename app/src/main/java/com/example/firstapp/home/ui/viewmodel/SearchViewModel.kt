package com.example.firstapp.home.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.search.model.SearchResult
import com.example.firstapp.search.model.SearchType
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.firstapp.search.domain.repository.SearchRepository

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<SearchResult?>(null)
    val searchResults: StateFlow<SearchResult?> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var currentSearchJob: kotlinx.coroutines.Job? = null

    fun search(query: String, type: SearchType = SearchType.GLOBAL) {
        currentSearchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = null
            _isSearching.value = false
            return
        }
        currentSearchJob = viewModelScope.launch {
            delay(300)
            _isSearching.value = true
            repository.search(query, type).collect { result ->
                if (result is Resource.Success) _searchResults.value = result.data
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        currentSearchJob?.cancel()
        _searchResults.value = null
        _isSearching.value = false
    }
}
