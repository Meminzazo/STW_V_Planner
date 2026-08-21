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
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: VBucksRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Long = checkNotNull(savedStateHandle["accountId"])

    // La cuenta solo cambia de tipo (principal/dependiente) rara vez; basta pedirla una vez.
    private val accountFlow = flow { emit(repository.getAccountById(accountId)) }

    val state: StateFlow<HistoryState> = combine(
        repository.getTransactions(accountId),
        repository.getAccountsByParent(accountId), // vacío si esta cuenta ES la dependiente
        accountFlow
    ) { transactions, dependents, account ->
        val isDependent = account?.parentAccountId != null
        val dependentIds = dependents.map { it.id }.toSet()

        val totalsByDependent = dependents.associate { dep ->
            dep.id to transactions.filter {
                it.source == VBucksSource.GIFT &&
                        it.type == TransactionType.SPEND &&
                        it.receiverAccountId == dep.id
            }.sumOf { it.amount }
        }

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
            totalsByDependent = totalsByDependent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryState())
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
    val totalsByDependent: Map<Long, Int> = emptyMap()
)