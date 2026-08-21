package com.meminzazo.stwvplanner.presentation.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: VBucksRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val accountId: Long = checkNotNull(savedStateHandle["accountId"])

    val otherAccounts = repository.getAccounts()
        .map { accounts -> accounts.filter { it.id != accountId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _recipientName = MutableStateFlow("")
    val recipientName = _recipientName.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount = _amount.asStateFlow()

    fun onRecipientNameChange(value: String) { _recipientName.value = value }
    fun onDescriptionChange(value: String) { _description.value = value }
    fun onAmountChange(value: String) { _amount.value = value }

    fun onSaveClick() {
        viewModelScope.launch {
            val amountInt = _amount.value.toIntOrNull() ?: 0
            if (amountInt <= 0) {
                _uiEvent.emit(UiEvent.ShowError("Monto inválido"))
                return@launch
            }

            val transaction = Transaction(
                accountId = accountId,
                amount = amountInt,
                type = TransactionType.SPEND,
                source = VBucksSource.GIFT,
                description = _description.value,
                date = System.currentTimeMillis(),
                recipientAccountName = _recipientName.value
            )
            repository.insertTransaction(transaction)
            _uiEvent.emit(UiEvent.SaveSuccess)
        }
    }

    sealed class UiEvent {
        data class ShowError(val message: String) : UiEvent()
        object SaveSuccess : UiEvent()
    }
}
