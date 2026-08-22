package com.meminzazo.stwvplanner.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: VBucksRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Long = checkNotNull(savedStateHandle["accountId"])

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate = _selectedDate.asStateFlow()

    fun onMonthChange(delta: Int) {
        val newDate = (_selectedDate.value.clone() as Calendar).apply {
            add(Calendar.MONTH, delta)
        }
        _selectedDate.value = newDate
    }

    // La cuenta solo cambia de tipo (principal/dependiente) rara vez; basta pedirla una vez.
    private val accountFlow = flow { emit(repository.getAccountById(accountId)) }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<HistoryState> = combine(
        _selectedDate.flatMapLatest { date ->
            val start = (date.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = (date.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            repository.getTransactionsInRange(accountId, start, end)
        },
        repository.getAccountsByParent(accountId),
        accountFlow,
        _selectedDate
    ) { transactions, dependents, account, date ->
        val isDependent = account?.parentAccountId != null
        val dependentIds = dependents.map { it.id }.toSet()

        val totalsByDependent = dependents.associate { dep ->
            dep.id to transactions.filter {
                it.source == VBucksSource.GIFT &&
                        it.type == TransactionType.SPEND &&
                        it.receiverAccountId == dep.id
            }.sumOf { it.amount }
        }

        val monthName = android.text.format.DateFormat.format("MMMM yyyy", date).toString()
            .replaceFirstChar { it.uppercase() }

        HistoryState(
            accountName = account?.name.orEmpty(),
            isDependent = isDependent,
            transactions = transactions,
            totalDaily = transactions.filter { it.source == VBucksSource.DAILY }.sumOf { it.amount },
            totalAlert = transactions.filter { it.source == VBucksSource.ALERT }.sumOf { it.amount },
            totalExternal = transactions.filter { it.source == VBucksSource.EXTERNAL }.sumOf { it.amount },
            totalOthers = transactions.filter {
                it.source != VBucksSource.DAILY &&
                        it.source != VBucksSource.ALERT &&
                        it.source != VBucksSource.EXTERNAL &&
                        !(it.source == VBucksSource.GIFT && it.type == TransactionType.SPEND && it.receiverAccountId in dependentIds)
            }.sumOf { if (it.type == TransactionType.SPEND) -it.amount else it.amount },
            dependents = dependents,
            totalsByDependent = totalsByDependent,
            selectedMonthName = monthName,
            selectedMonth = date.get(Calendar.MONTH),
            selectedYear = date.get(Calendar.YEAR)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryState())

    fun onDeleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun onUpdateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun onAddTransaction(
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
}

data class HistoryState(
    val accountName: String = "",
    val isDependent: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val totalDaily: Int = 0,
    val totalAlert: Int = 0,
    val totalExternal: Int = 0,
    val totalOthers: Int = 0,
    val dependents: List<Account> = emptyList(),
    val totalsByDependent: Map<Long, Int> = emptyMap(),
    val selectedMonthName: String = "",
    val selectedMonth: Int = 0,
    val selectedYear: Int = 0
)
