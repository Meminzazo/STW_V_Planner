package com.meminzazo.stwvplanner.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.meminzazo.stwvplanner.domain.model.User
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            val user = firebaseUser?.let {
                User(
                    id = it.uid,
                    email = it.email,
                    displayName = it.displayName ?: it.email?.split("@")?.get(0),
                    photoUrl = it.photoUrl?.toString()
                )
            }
            trySend(user)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Usuario nulo"))
            Result.success(mapFirebaseUser(firebaseUser))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Error en inicio de sesión"))
            Result.success(mapFirebaseUser(firebaseUser))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Result.failure(Exception("Error al crear cuenta"))
            Result.success(mapFirebaseUser(firebaseUser))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
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
