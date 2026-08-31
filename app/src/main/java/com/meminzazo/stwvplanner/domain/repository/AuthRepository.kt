package com.meminzazo.stwvplanner.domain.repository

import com.meminzazo.stwvplanner.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signInLocally(): Result<User>
    suspend fun isUserLocal(): Boolean
    suspend fun signOut()
}
