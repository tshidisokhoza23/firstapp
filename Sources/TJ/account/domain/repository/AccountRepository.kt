package com.tpsk.tj.account.domain.repository

import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.common.model.PrivacySettings
import com.tpsk.tj.common.model.User
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.common.data.api.UploadResponse
import kotlinx.coroutines.flow.Flow
import android.content.Context
import android.net.Uri
import java.io.File

interface AccountRepository {
    fun getCurrentUser(): Flow<Resource<User>>
    fun getUserById(userId: String): Flow<Resource<User>>
    fun updateUser(displayName: String?, bio: String?, location: String?, avatarUrl: String?, bannerUrl: String? = null, isArtist: Boolean? = null): Flow<Resource<User>>
    fun deleteAccount(): Flow<Resource<Boolean>>
    fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>>

    fun getPrivacySettings(): Flow<Resource<PrivacySettings>>
    fun updatePrivacySettings(settings: PrivacySettings): Flow<Resource<Boolean>>
    fun blockUser(userId: String): Flow<Resource<Boolean>>
    fun unblockUser(userId: String): Flow<Resource<Boolean>>
    fun followUser(userId: String): Flow<Resource<Boolean>>
    fun unfollowUser(userId: String): Flow<Resource<Boolean>>
    fun getFollowers(userId: String): Flow<Resource<List<Contact>>>
    fun getFollowing(userId: String): Flow<Resource<List<Contact>>>
    fun getBlockedUsers(): Flow<Resource<List<Contact>>>
    suspend fun getPublicKey(userId: String): String?
    suspend fun setPublicKey(publicKey: String): Boolean
    fun reportUser(userId: String, reason: String): Flow<Resource<Boolean>>
    fun upsertPushToken(token: String, deviceId: String): Flow<Resource<Boolean>>
    fun uploadFile(file: File, folder: String? = null): Flow<Resource<UploadResponse>>
    fun uploadUri(context: Context, uri: Uri, folder: String? = null): Flow<Resource<UploadResponse>>
}
