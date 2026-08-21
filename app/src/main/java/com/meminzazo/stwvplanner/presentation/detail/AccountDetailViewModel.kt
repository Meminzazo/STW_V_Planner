package com.meminzazo.stwvplanner.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.*
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    private val repository: VBucksRepository,
    private val addAccountUseCase: com.meminzazo.stwvplanner.domain.usecase.AddAccountUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Long = checkNotNull(savedStateHandle["accountId"])

    val account = flow {
        emit(repository.getAccountById(accountId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val balance = repository.getBalance(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _isDailyRegistered = MutableStateFlow(false)
    val isDailyRegistered = _isDailyRegistered.asStateFlow()

    init {
        checkDailyMission()
    }

    private fun checkDailyMission() {
        viewModelScope.launch {
            val count = repository.countDailyMissionsInDate(accountId, System.currentTimeMillis())
            _isDailyRegistered.value = count > 0
        }
    }

    fun onAddDailyClick(amount: Int) {
        viewModelScope.launch {
            if (_isDailyRegistered.value) {
                _uiEvent.emit(UiEvent.ShowError("Ya has registrado la misión diaria de hoy"))
                return@launch
            }
            
            val transaction = Transaction(
                accountId = accountId,
                amount = amount,
                type = TransactionType.EARN,
                source = VBucksSource.DAILY,
                description = "Misión Diaria",
                date = System.currentTimeMillis()
            )
            repository.insertTransaction(transaction)
            _isDailyRegistered.value = true
        }
    }

    fun onAddAlertClick() {
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

    fun onAddExternalClick(amount: Int, description: String) {
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountId,
                amount = amount,
                type = TransactionType.EARN,
                source = VBucksSource.EXTERNAL,
                description = description,
                date = System.currentTimeMillis()
            )
            repository.insertTransaction(transaction)
        }
    }

    // Lógica para obtener balances por relación (Solo dependientes de esta cuenta)
    val dependentAccounts = repository.getAccountsByParent(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onCreateDependentAccount(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            addAccountUseCase(name = name, parentAccountId = accountId)
        }
    }

    val earningsDesglosadas = repository.getTransactions(accountId)
        .map { transactions ->
            transactions.filter { it.type == TransactionType.EARN }
                .groupBy { it.source }
                .mapValues { it.value.sumOf { t -> t.amount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val expenseDistribution = repository.getTransactions(accountId)
        .map { transactions ->
            transactions.filter { it.type == TransactionType.SPEND }
                .groupBy { 
                    it.recipientAccountName ?: it.source.name
                }
                .mapValues { it.value.sumOf { t -> t.amount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun getRelationBalance(otherId: Long): Flow<Int> {
        return combine(
            repository.getVBucksReceivedFrom(accountId, otherId),
            repository.getVBucksSentTo(accountId, otherId)
        ) { received, sent -> received - sent }
    }

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
    }
}
