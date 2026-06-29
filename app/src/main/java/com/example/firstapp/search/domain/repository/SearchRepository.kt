package com.example.firstapp.search.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.search.model.SearchFilter
import com.example.firstapp.search.model.SearchResult
import com.example.firstapp.search.model.SearchType
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun search(query: String, type: SearchType = SearchType.GLOBAL, filter: SearchFilter = SearchFilter.ALL): Flow<Resource<SearchResult>>
}
