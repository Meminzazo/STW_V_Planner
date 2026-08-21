package com.meminzazo.stwvplanner.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: VBucksRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Long = checkNotNull(savedStateHandle["accountId"])

    val state: StateFlow<HistoryState> = repository.getTransactions(accountId)
        .map { transactions ->
            HistoryState(
                transactions = transactions,
                totalDaily = transactions.filter { it.source == VBucksSource.DAILY }.sumOf { it.amount },
                totalAlert = transactions.filter { it.source == VBucksSource.ALERT }.sumOf { it.amount },
                totalExternal = transactions.filter { it.source == VBucksSource.EXTERNAL }.sumOf { it.amount },
                totalOthers = transactions.filter { 
                    it.source != VBucksSource.DAILY && 
                    it.source != VBucksSource.ALERT && 
                    it.source != VBucksSource.EXTERNAL 
                }.sumOf { if (it.type == TransactionType.SPEND) -it.amount else it.amount }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryState())
}

data class HistoryState(
    val transactions: List<Transaction> = emptyList(),
    val totalDaily: Int = 0,
    val totalAlert: Int = 0,
    val totalExternal: Int = 0,
    val totalOthers: Int = 0
)
