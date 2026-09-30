package com.tpsk.tj.home.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.search.model.SearchFilter
import com.tpsk.tj.search.model.SearchResult
import com.tpsk.tj.search.model.SearchType
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.tpsk.tj.search.domain.repository.SearchRepository

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<SearchResult?>(null)
    val searchResults: StateFlow<SearchResult?> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _activeFilter = MutableStateFlow(SearchFilter.ALL)
    val activeFilter: StateFlow<SearchFilter> = _activeFilter.asStateFlow()

    private var currentSearchJob: kotlinx.coroutines.Job? = null
    private var lastQuery = ""
    private var lastType = SearchType.GLOBAL

    fun search(query: String, type: SearchType = SearchType.GLOBAL, filter: SearchFilter = _activeFilter.value) {
        lastQuery = query
        lastType = type
        _activeFilter.value = filter
        
        currentSearchJob?.cancel()
        if (query.isBlank() && filter == SearchFilter.ALL) {
            _searchResults.value = null
            _isSearching.value = false
            return
        }
        currentSearchJob = viewModelScope.launch {
            delay(300)
            _isSearching.value = true
            repository.search(query, type, filter).collect { result ->
                if (result is Resource.Success) _searchResults.value = result.data
                _isSearching.value = false
            }
        }
    }

    fun setFilter(filter: SearchFilter) {
        search(lastQuery, lastType, filter)
    }

    fun clearSearch() {
        currentSearchJob?.cancel()
        _searchResults.value = null
        _isSearching.value = false
    }
}
