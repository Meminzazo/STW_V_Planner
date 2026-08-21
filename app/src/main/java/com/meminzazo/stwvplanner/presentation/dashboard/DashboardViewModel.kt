package com.meminzazo.stwvplanner.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import com.meminzazo.stwvplanner.domain.usecase.AddAccountUseCase
import com.meminzazo.stwvplanner.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: VBucksRepository,
    private val authRepository: AuthRepository,
    private val addTransactionUseCase: AddTransactionUseCase,
    private val addAccountUseCase: AddAccountUseCase
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onSignOutClick() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun onCreateAccountClick(name: String, isMain: Boolean) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            addAccountUseCase(name, isMain)
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

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
    }
}
