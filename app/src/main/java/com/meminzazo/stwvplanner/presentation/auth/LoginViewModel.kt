package com.meminzazo.stwvplanner.presentation.auth

import android.content.Context
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

    private val _isLoginMode = MutableStateFlow(true)
    val isLoginMode = _isLoginMode.asStateFlow()

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onEmailChange(value: String) { _email.value = value }
    fun onPasswordChange(value: String) { _password.value = value }
    fun toggleAuthMode() { _isLoginMode.value = !_isLoginMode.value }

    fun onSignInWithGoogle(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(com.meminzazo.stwvplanner.R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                handleGoogleSignInResult(result)
            } catch (e: GetCredentialException) {
                _uiEvent.emit(UiEvent.ShowError("Error: ${e.message}"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error inesperado: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onEmailAuthClick() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Completa todos los campos")) }
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = if (_isLoginMode.value) {
                authRepository.signInWithEmail(_email.value, _password.value)
            } else {
                authRepository.signUpWithEmail(_email.value, _password.value)
            }
            
            result.onFailure {
                _uiEvent.emit(UiEvent.ShowError(it.message ?: "Error en autenticación"))
            }
            _isLoading.value = false
        }
    }

    fun onForgotPasswordClick() {
        if (_email.value.isBlank()) {
            viewModelScope.launch { _uiEvent.emit(UiEvent.ShowError("Ingresa tu correo para recuperar la contraseña")) }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.sendPasswordResetEmail(_email.value)
            _uiEvent.emit(UiEvent.ShowError(if (result.isSuccess) "Correo de recuperación enviado" else "Error al enviar correo"))
            _isLoading.value = false
        }
    }

    private suspend fun handleGoogleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential
        val googleIdTokenCredential = try {
            if (credential is GoogleIdTokenCredential) credential
            else GoogleIdTokenCredential.createFrom(credential.data)
        } catch (e: Exception) { null }

        if (googleIdTokenCredential != null) {
            val authResult = authRepository.signInWithGoogle(googleIdTokenCredential.idToken)
            authResult.onFailure { _uiEvent.emit(UiEvent.ShowError("Error: ${it.message}")) }
        } else {
            _uiEvent.emit(UiEvent.ShowError("Tipo de credencial no reconocido"))
        }
    }

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
    }
}
