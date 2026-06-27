package com.example.firstapp.common.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.common.data.api.DevResetResponse
import com.example.firstapp.common.util.ContactUtils
import kotlinx.coroutines.flow.Flow

interface SystemRepository {
    suspend fun clearCache()
    suspend fun clearAllData()
    suspend fun devResetAndSeed(contacts: List<ContactUtils.ContactInfo>): Flow<Resource<DevResetResponse>>
}
