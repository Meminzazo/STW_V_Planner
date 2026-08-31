package com.meminzazo.stwvplanner.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.meminzazo.stwvplanner.domain.model.User
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val _localModeFlow = MutableStateFlow(prefs.getBoolean("is_local_mode", false))

    override val currentUser: Flow<User?> = combine(
        callbackFlow {
            val authListener = FirebaseAuth.AuthStateListener { auth ->
                trySend(auth.currentUser)
            }
            firebaseAuth.addAuthStateListener(authListener)
            awaitClose { firebaseAuth.removeAuthStateListener(authListener) }
        },
        _localModeFlow
    ) { firebaseUser, isLocal ->
        if (firebaseUser != null) {
            mapFirebaseUser(firebaseUser)
        } else if (isLocal) {
            User(id = "local_user", email = "offline@local", displayName = "Invitado", photoUrl = null)
        } else {
            null
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Usuario nulo"))
            setLocalMode(false)
            Result.success(mapFirebaseUser(firebaseUser))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInLocally(): Result<User> {
        setLocalMode(true)
        return Result.success(User(id = "local_user", email = "offline@local", displayName = "Invitado", photoUrl = null))
    }

    override suspend fun isUserLocal(): Boolean {
        return _localModeFlow.value
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        setLocalMode(false)
    }

    private fun setLocalMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_local_mode", enabled).apply()
        _localModeFlow.value = enabled
    }

    private fun mapFirebaseUser(firebaseUser: com.google.firebase.auth.FirebaseUser): User {
        return User(
            id = firebaseUser.uid,
            email = firebaseUser.email,
            displayName = firebaseUser.displayName ?: firebaseUser.email?.split("@")?.get(0),
            photoUrl = firebaseUser.photoUrl?.toString()
        )
    }
}
