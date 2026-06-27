package com.example.firstapp.search.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.search.model.SearchResult
import com.example.firstapp.search.model.SearchType
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun search(query: String, type: SearchType = SearchType.GLOBAL): Flow<Resource<SearchResult>>
}
