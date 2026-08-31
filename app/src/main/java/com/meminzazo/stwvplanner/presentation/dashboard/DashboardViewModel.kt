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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel central para la gestión de cuentas y sincronización en la nube.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: VBucksRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val addAccountUseCase: AddAccountUseCase
) : ViewModel() {

    // Lista de cuentas principales para el Dashboard
    val accounts: StateFlow<List<Account>> = repository.getMainAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Todas las cuentas (necesario para filtrar dependientes localmente)
    val allAccounts: StateFlow<List<Account>> = repository.getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var lastActionTime = 0L
    private val ACTION_COOLDOWN = 15000L // 15 segundos entre peticiones de nube

    // Eventos de una sola vez (errores o avisos)
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    /**
     * Verifica si se puede realizar una acción de red (Cooldown).
     */
    private fun isActionAllowed(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastActionTime < ACTION_COOLDOWN) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowError("Por favor, espera unos segundos"))
            }
            return false
        }
        lastActionTime = now
        return true
    }

    fun onSignOutClick() {
        viewModelScope.launch { authRepository.signOut() }
    }

    /**
     * Sincronización diferencial (solo lo pendiente).
     */
    fun onSyncClick() {
        if (!isActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepository.currentUser.first()
            if (user != null) {
                val result = syncRepository.syncAll(user.id)
                val message = if (result.isSuccess) "Sincronización completada" 
                              else "Error: ${result.exceptionOrNull()?.message}"
                _uiEvent.emit(UiEvent.ShowError(message))
            }
            _isLoading.value = false
        }
    }

    /**
     * Respaldo completo de la DB actual.
     */
    fun onBackupClick() {
        if (!isActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepository.currentUser.first()
            if (user != null) {
                val result = syncRepository.backupFullDatabase(user.id)
                _uiEvent.emit(UiEvent.ShowError(if (result.isSuccess) "Respaldo total guardado" else "Error al respaldar"))
            }
            _isLoading.value = false
        }
    }

    /**
     * Restauración total (borra local y pone lo de la nube).
     */
    fun onRestoreClick() {
        if (!isActionAllowed()) return
        viewModelScope.launch {
            _isLoading.value = true
            val user = authRepository.currentUser.first()
            if (user != null) {
                val result = syncRepository.restoreFullDatabase(user.id)
                _uiEvent.emit(UiEvent.ShowError(if (result.isSuccess) "Restauración completada" else "Error al restaurar"))
            }
            _isLoading.value = false
        }
    }

    /**
     * Genera un código de 6 dígitos para que un amigo descargue los datos.
     */
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
                    _uiEvent.emit(UiEvent.ShowError("Error al generar código"))
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Importa datos desde un código de transferencia de un amigo.
     */
    fun onImportWithCode(code: String) {
        if (!isActionAllowed()) return
        viewModelScope.launch {
            if (code.length != 8) return@launch
            _isLoading.value = true
            val result = syncRepository.restoreFromTransferCode(code)
            _uiEvent.emit(UiEvent.ShowError(if (result.isSuccess) "Registros importados con éxito" else "Código inválido o expirado"))
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
            addTransactionUseCase(transaction)
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
        viewModelScope.launch { repository.deleteAccount(accountId) }
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
            repository.insertTransaction(transaction)
        }
    }

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        data class ShowTransferCode(val code: String) : UiEvent()
    }
}
