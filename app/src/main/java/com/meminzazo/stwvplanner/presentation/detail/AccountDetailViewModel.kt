package com.meminzazo.stwvplanner.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.*
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DependentRelation(
    val account: Account,
    val totalBalance: Int,
    val monthlyBalance: Int,
    val monthlyReceived: Int
)

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

    private val startOfMonth: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private val endOfMonth: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

    val earningsDesglosadas = repository.getTransactions(accountId)
        .map { transactions ->
            transactions.filter { it.type == TransactionType.EARN }
                .groupBy { it.source }
                .mapValues { it.value.sumOf { t -> t.amount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val earningsDesglosadasMensual = repository.getTransactionsInRange(accountId, startOfMonth, endOfMonth)
        .map { transactions ->
            transactions.filter { it.type == TransactionType.EARN }
                .groupBy { it.source }
                .mapValues { it.value.sumOf { t -> t.amount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val earningsTransactions = repository.getTransactions(accountId)
        .map { transactions -> transactions.filter { it.type == TransactionType.EARN } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val earningsTransactionsMensual = repository.getTransactionsInRange(accountId, startOfMonth, endOfMonth)
        .map { transactions -> transactions.filter { it.type == TransactionType.EARN } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseDistribution = repository.getTransactions(accountId)
        .map { transactions ->
            transactions.filter { it.type == TransactionType.SPEND }
                .groupBy {
                    it.recipientAccountName ?: it.source.name
                }
                .mapValues { it.value.sumOf { t -> t.amount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val expenseDistributionMensual = repository.getTransactionsInRange(accountId, startOfMonth, endOfMonth)
        .map { transactions ->
            transactions.filter { it.type == TransactionType.SPEND }
                .groupBy {
                    it.recipientAccountName ?: it.source.name
                }
                .mapValues { it.value.sumOf { t -> t.amount } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val expenseTransactions = repository.getTransactions(accountId)
        .map { transactions -> transactions.filter { it.type == TransactionType.SPEND } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseTransactionsMensual = repository.getTransactionsInRange(accountId, startOfMonth, endOfMonth)
        .map { transactions -> transactions.filter { it.type == TransactionType.SPEND } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total de pavos gastados (egresos)
    val totalExpenses = repository.getTransactions(accountId)
        .map { transactions ->
            transactions.filter { it.type == TransactionType.SPEND }.sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalExpensesMensual = repository.getTransactionsInRange(accountId, startOfMonth, endOfMonth)
        .map { transactions ->
            transactions.filter { it.type == TransactionType.SPEND }.sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Total de pavos ingresados (ingresos)
    val totalIncome = repository.getTransactions(accountId)
        .map { transactions ->
            transactions.filter { it.type == TransactionType.EARN }.sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalIncomeMensual = repository.getTransactionsInRange(accountId, startOfMonth, endOfMonth)
        .map { transactions ->
            transactions.filter { it.type == TransactionType.EARN }.sumOf { it.amount }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sharingPayload = combine(
        account,
        repository.getTransactions(accountId).take(1)
    ) { acc, txs ->
        if (acc != null) {
            com.meminzazo.stwvplanner.domain.model.SharingPayload(acc, txs.take(50))
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Lógica para obtener balances por relación (Solo dependientes de esta cuenta)
    val dependentAccounts = repository.getAccountsByParent(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedDependents = repository.getDeletedAccountsByParent(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Balance de Compensación Directa
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val dependentRelations = dependentAccounts.flatMapLatest { accounts ->
        if (accounts.isEmpty()) return@flatMapLatest flowOf(emptyList<DependentRelation>())

        val perDependentFlows = accounts.map { dep ->
            combine(
                repository.getVBucksSentTo(accountId, dep.id),
                repository.getVBucksSentToInRange(accountId, dep.id, startOfMonth, endOfMonth)
            ) { total, monthly -> dep to (total to monthly) }
        }

        combine(perDependentFlows) { entries ->
            val totalAll = entries.sumOf { it.second.first }
            val monthlyAll = entries.sumOf { it.second.second }
            
            entries.map { (dep, amounts) ->
                val (totalReceived, monthlyReceived) = amounts
                DependentRelation(
                    account = dep,
                    totalBalance = totalAll - 2 * totalReceived,
                    monthlyBalance = monthlyAll - 2 * monthlyReceived,
                    monthlyReceived = monthlyReceived
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onCreateDependentAccount(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            addAccountUseCase(name = name, parentAccountId = accountId)
        }
    }

    fun onDeleteAccountClick(id: Long) {
        viewModelScope.launch {
            repository.deleteAccount(id)
        }
    }

    fun onRestoreAccountClick(id: Long) {
        viewModelScope.launch {
            repository.restoreAccount(id)
        }
    }

    fun onRenameAccountClick(id: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isBlank()) return@launch
            val acc = repository.getAccountById(id)
            if (acc != null) {
                repository.updateAccount(acc.copy(name = newName))
            }
        }
    }

    fun onManualEntryClick(
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
                if (source == VBucksSource.DAILY) {
                    checkDailyMission()
                }
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.ShowError(e.message ?: "Error al guardar el registro"))
            }
        }
    }

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
    }
}
