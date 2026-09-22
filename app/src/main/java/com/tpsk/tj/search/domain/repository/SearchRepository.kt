package com.tpsk.tj.search.domain.repository

import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.search.model.SearchFilter
import com.tpsk.tj.search.model.SearchResult
import com.tpsk.tj.search.model.SearchType
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun search(query: String, type: SearchType = SearchType.GLOBAL, filter: SearchFilter = SearchFilter.ALL): Flow<Resource<SearchResult>>
}
