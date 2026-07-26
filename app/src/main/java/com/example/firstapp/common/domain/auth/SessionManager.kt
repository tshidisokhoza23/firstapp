package com.example.firstapp.common.domain.auth

import com.example.firstapp.common.model.User
import kotlinx.coroutines.flow.StateFlow

interface SessionManager {
    val authState: StateFlow<Boolean>
    val currentUser: StateFlow<User?>

    fun getToken(): String?
    fun getRefreshToken(): String?
    fun getUserId(): String?
    fun isLoggedIn(): Boolean

    fun saveSession(token: String, refreshToken: String, user: User?)
    fun updateCurrentUser(user: User)
    fun clearSession()
    
    fun getPrivateKey(): String?
    fun setPrivateKey(key: String)
    fun getPublicKey(): String?
    fun setPublicKey(key: String)
    
    fun getDbPassphrase(): String
}
