package com.meminzazo.stwvplanner.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import com.meminzazo.stwvplanner.domain.repository.SyncRepository
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import com.meminzazo.stwvplanner.domain.usecase.AddAccountUseCase
import com.meminzazo.stwvplanner.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: VBucksRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val addAccountUseCase: AddAccountUseCase
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.getMainAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<Account>> = repository.getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var lastActionTime = 0L
    private val ACTION_COOLDOWN = 15000L // 15 segundos de espera entre acciones de nube

    private fun isActionAllowed(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < ACTION_COOLDOWN) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("Por favor, espera unos segundos antes de volver a intentar"))
            }
            return false
        }
        lastActionTime = now
        return true
    }

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onSignOutClick() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun onSyncClick() {
        if (!isActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepository.currentUser.first()
            if (user != null) {
                val result = syncRepository.syncAll(user.id)
                if (result.isSuccess) {
                    _uiEvent.emit(UiEvent.ShowError("Sincronización completada"))
                } else {
                    _uiEvent.emit(UiEvent.ShowError("Error al sincronizar: ${result.exceptionOrNull()?.message}"))
                }
            }
            _isLoading.value = false
        }
    }

    fun onBackupClick() {
        if (!isActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepository.currentUser.first()
            if (user != null) {
                val result = syncRepository.backupFullDatabase(user.id)
                if (result.isSuccess) {
                    _uiEvent.emit(UiEvent.ShowError("Respaldo total guardado en la nube"))
                } else {
                    _uiEvent.emit(UiEvent.ShowError("Error al respaldar: ${result.exceptionOrNull()?.message}"))
                }
            }
            _isLoading.value = false
        }
    }

    fun onRestoreClick() {
        if (!isActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepository.currentUser.first()
            if (user != null) {
                val result = syncRepository.restoreFullDatabase(user.id)
                if (result.isSuccess) {
                    _uiEvent.emit(UiEvent.ShowError("Restauración completada con éxito"))
                } else {
                    _uiEvent.emit(UiEvent.ShowError("Error al restaurar: ${result.exceptionOrNull()?.message}"))
                }
            }
            _isLoading.value = false
        }
    }

    fun onGenerateTransferCode() {
        if (!isActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepository.currentUser.first()
            if (user != null) {
                val result = syncRepository.generateTransferCode(user.id)
                if (result.isSuccess) {
                    _uiEvent.emit(UiEvent.ShowTransferCode(result.getOrNull() ?: ""))
                } else {
                    _uiEvent.emit(UiEvent.ShowError("Error al generar código: ${result.exceptionOrNull()?.message}"))
                }
            }
            _isLoading.value = false
        }
    }

    fun onImportWithCode(code: String) {
        if (!isActionAllowed()) return
        viewModelScope.launch {
            if (code.length != 6) {
                _uiEvent.emit(UiEvent.ShowError("El código debe ser de 6 dígitos"))
                return@launch
            }
            _isLoading.value = true
            val result = syncRepository.restoreFromTransferCode(code)
            if (result.isSuccess) {
                _uiEvent.emit(UiEvent.ShowError("Registros importados con éxito"))
            } else {
                _uiEvent.emit(UiEvent.ShowError("Error: ${result.exceptionOrNull()?.message}"))
            }
            _isLoading.value = false
        }
    }

    fun onCreateAccountClick(name: String, isMain: Boolean = true) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            addAccountUseCase(name = name, isMain = isMain)
        }
    }

    fun onRenameAccountClick(accountId: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isBlank()) return@launch
            val account = repository.getAccountById(accountId)
            if (account != null) {
                repository.updateAccount(account.copy(name = newName))
            }
        }
    }

    fun onAddDailyClick(accountId: Long, amount: Int) {
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountId,
                amount = amount,
                type = TransactionType.EARN,
                source = VBucksSource.DAILY,
                description = "Misión Diaria",
                date = System.currentTimeMillis()
            )
            val result = addTransactionUseCase(transaction)
            result.onFailure {
                _uiEvent.emit(UiEvent.ShowError(it.message ?: "Error desconocido"))
            }
        }
    }

    fun onAddAlertClick(accountId: Long) {
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountId,
                amount = 50,
                type = TransactionType.EARN,
                source = VBucksSource.ALERT,
                description = "Alerta de Misión",
                date = System.currentTimeMillis()
            )
            repository.insertTransaction(transaction)
        }
    }

    fun onDeleteAccountClick(accountId: Long) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
        }
    }

    fun onImportFromQR(json: String) {
        viewModelScope.launch {
            val payload = com.meminzazo.stwvplanner.presentation.common.QRSharing.parseQRCode(json)
            if (payload != null) {
                // Insertar cuenta (sin ID para que genere uno nuevo local)
                val newAccountId = repository.insertAccount(payload.account.copy(id = 0))
                // Insertar transacciones vinculadas
                payload.transactions.forEach { tx ->
                    repository.insertTransaction(tx.copy(id = 0, accountId = newAccountId))
                }
                _uiEvent.emit(UiEvent.ShowError("Cuenta importada: ${payload.account.name}"))
            } else {
                _uiEvent.emit(UiEvent.ShowError("Código QR inválido"))
            }
        }
    }

    fun onManualEntryClick(
        accountId: Long,
        amount: Int,
        type: TransactionType,
        source: VBucksSource,
        description: String,
        date: Long,
        receiverId: Long? = null,
        receiverName: String? = null
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountId,
                amount = amount,
                type = type,
                source = source,
                description = description.ifBlank { 
                    if (type == TransactionType.SPEND && receiverName != null) "Regalo a $receiverName"
                    else source.name 
                },
                date = date,
                receiverAccountId = receiverId,
                recipientAccountName = receiverName
            )
            try {
                repository.insertTransaction(transaction)
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError(e.message ?: "Error al guardar el registro"))
            }
        }
    }

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowTransferCode(val code: String) : UiEvent()
    }
}
