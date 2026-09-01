package com.meminzazo.stwvplanner.data.repository

import android.content.Context
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.meminzazo.stwvplanner.domain.model.User
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val appCheck: FirebaseAppCheck,
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val _localModeFlow = MutableStateFlow(prefs.getBoolean("is_local_mode", false))
    private val _bannerMinimizedFlow = MutableStateFlow(prefs.getBoolean("guest_banner_minimized", false))

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

    override fun isGuestBannerMinimized(): Flow<Boolean> = _bannerMinimizedFlow

    override suspend fun setGuestBannerMinimized(minimized: Boolean) {
        prefs.edit().putBoolean("guest_banner_minimized", minimized).apply()
        _bannerMinimizedFlow.value = minimized
    }

    override suspend fun getAppCheckDebugToken(): String? {
        return try {
            // El SDK de Firebase usa un nombre de archivo dinámico: com.google.firebase.appcheck.debug.store.[PERSISTENCE_KEY]
            // Escaneamos la carpeta de shared_prefs para encontrarlo.
            val sharedPrefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                val debugPrefsFile = sharedPrefsDir.listFiles()?.find { 
                    it.name.startsWith("com.google.firebase.appcheck.debug.store") 
                }
                if (debugPrefsFile != null) {
                    val prefsName = debugPrefsFile.name.removeSuffix(".xml")
                    val debugPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
                    return debugPrefs.getString("com.google.firebase.appcheck.debug.DEBUG_SECRET", null)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun ensureAppCheckTokenGenerated() {
        try {
            // Forzamos la obtención de un token (aunque no lo usemos) para asegurar
            // que el proveedor de depuración se inicialice y genere el secreto.
            appCheck.getAppCheckToken(false).await()
        } catch (_: Exception) {
            // No importa si falla (ej. sin red), el objetivo es disparar la inicialización local.
        }
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
