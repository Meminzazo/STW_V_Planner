package com.meminzazo.stwvplanner.domain.repository

import com.meminzazo.stwvplanner.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val isUserLocal: Flow<Boolean>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signInLocally(): Result<User>
    suspend fun signInAnonymously(): Result<User>
    suspend fun signOut()
    
    fun isGuestBannerMinimized(): Flow<Boolean>
    suspend fun setGuestBannerMinimized(minimized: Boolean)
    
    suspend fun getAppCheckDebugToken(): String?
    suspend fun ensureAppCheckTokenGenerated()
    suspend fun checkAppCheckStatus(): Result<Unit>
}
