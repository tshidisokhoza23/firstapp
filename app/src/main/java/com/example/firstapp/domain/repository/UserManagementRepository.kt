package com.example.firstapp.domain.repository

import com.example.firstapp.common.data.api.UploadResponse
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.model.PrivacySettings
import com.example.firstapp.common.util.Resource
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
