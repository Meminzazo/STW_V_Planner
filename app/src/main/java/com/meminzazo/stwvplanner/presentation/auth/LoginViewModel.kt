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

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onSignInClick(context: Context) {
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
                handleSignInResult(result)
            } catch (e: GetCredentialException) {
                _uiEvent.emit(UiEvent.ShowError("Error CredentialManager: ${e.message} (${e.javaClass.simpleName})"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError("Error: ${e.message} (${e.javaClass.simpleName})"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse) {
        val credential = result.credential
        
        val googleIdTokenCredential = try {
            if (credential is GoogleIdTokenCredential) {
                credential
            } else {
                GoogleIdTokenCredential.createFrom(credential.data)
            }
        } catch (e: Exception) {
            null
        }

        if (googleIdTokenCredential != null) {
            val idToken = googleIdTokenCredential.idToken
            val authResult = authRepository.signInWithGoogle(idToken)
            authResult.onFailure {
                _uiEvent.emit(UiEvent.ShowError(it.message ?: "Error al iniciar sesión con Firebase"))
            }
        } else {
            _uiEvent.emit(UiEvent.ShowError("Tipo de credencial no reconocido: ${credential.type}"))
        }
    }

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
    }
}
