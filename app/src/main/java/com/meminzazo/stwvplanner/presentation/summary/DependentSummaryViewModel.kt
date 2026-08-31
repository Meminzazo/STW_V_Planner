package com.meminzazo.stwvplanner.presentation.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class MonthlyGifts(
    val monthName: String,
    val totalAmount: Int,
    val gifts: List<Transaction>
)

@HiltViewModel
class DependentSummaryViewModel @Inject constructor(
    private val repository: VBucksRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Long = checkNotNull(savedStateHandle["accountId"])

    val account = flow {
        emit(repository.getAccountById(accountId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val monthlyGifts = repository.getTransactions(accountId)
        .map { transactions ->
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            transactions
                .filter { it.type == TransactionType.EARN && it.source == com.meminzazo.stwvplanner.domain.model.VBucksSource.GIFT }
                .groupBy { 
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    sdf.format(cal.time).replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() }
                }
                .map { (month, txs) ->
                    MonthlyGifts(
                        monthName = month,
                        totalAmount = txs.sumOf { it.amount },
                        gifts = txs.sortedByDescending { it.date }
                    )
                }
                .sortedByDescending { it.gifts.firstOrNull()?.date ?: 0L }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
