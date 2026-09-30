package com.tpsk.tj.domain.repository

import com.tpsk.tj.common.data.api.UploadResponse
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.common.model.PrivacySettings
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.Flow
import java.io.File

interface UserManagementRepository {
    fun getContacts(): Flow<Resource<List<Contact>>>
    fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>>
    fun getPrivacySettings(): Flow<Resource<PrivacySettings>>
    fun updatePrivacySettings(settings: PrivacySettings): Flow<Resource<Boolean>>
    fun blockUser(userId: Int): Flow<Resource<Boolean>>
    fun unblockUser(userId: Int): Flow<Resource<Boolean>>
    fun getBlockedUsers(): Flow<Resource<List<Contact>>>
    suspend fun getPublicKey(userId: Int): String?
    suspend fun setPublicKey(publicKey: String): Boolean
    fun reportUser(userId: Int, reason: String): Flow<Resource<Boolean>>
    fun reportMessage(messageId: Int, reason: String): Flow<Resource<Boolean>>
    fun uploadFile(file: File, folder: String? = null): Flow<Resource<UploadResponse>>
}
