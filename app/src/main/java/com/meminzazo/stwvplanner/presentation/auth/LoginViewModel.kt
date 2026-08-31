package com.meminzazo.stwvplanner.presentation.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var lastActionTime = 0L
    private val AUTH_COOLDOWN = 3000L // 3 segundos entre intentos de login

    private fun isActionAllowed(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < AUTH_COOLDOWN) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Por favor, espera un momento")) }
            return false
        }
        lastActionTime = now
        return true
    }

    private fun Context.findActivity(): ComponentActivity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is ComponentActivity) return context
            context = context.baseContext
        }
        return null
    }

    fun onContinueAsGuest() {
        if (!isActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.signInLocally()
            _isLoading.value = false
        }
    }

    fun onSignInWithGoogle(context: Context) {
        if (!isActionAllowed()) return
        val activity = context.findActivity()
        if (activity == null) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Error interno: No se encontró la actividad")) }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val credentialManager = CredentialManager.create(activity)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(com.meminzazo.stwvplanner.R.string.default_web_client_id))
                    .setAutoSelectEnabled(false) // Desactivamos auto-select para evitar cancelaciones automáticas
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activity, request)
                handleGoogleSignInResult(result)
            } catch (e: GetCredentialException) {
                _uiEvent.emit(UiEvent.ShowError("Google: ${e.message}"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error inesperado: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun handleGoogleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            val res = authRepository.signInWithGoogle(credential.idToken)
            if (res.isFailure) {
                _uiEvent.emit(UiEvent.ShowError("Error al iniciar sesión: ${res.exceptionOrNull()?.message}"))
            }
        } else {
            _uiEvent.emit(UiEvent.ShowError("Tipo de credencial no soportado"))
        }
    }

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
    }
}
