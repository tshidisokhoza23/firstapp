package com.tpsk.tj.common.domain.repository

import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.common.data.api.DevResetResponse
import com.tpsk.tj.common.util.ContactUtils
import kotlinx.coroutines.flow.Flow

interface SystemRepository {
    suspend fun clearCache()
    suspend fun clearAllData()
    suspend fun devResetAndSeed(contacts: List<ContactUtils.ContactInfo>): Flow<Resource<DevResetResponse>>
}
