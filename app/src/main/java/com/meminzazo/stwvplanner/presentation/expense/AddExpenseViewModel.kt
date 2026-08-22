package com.meminzazo.stwvplanner.presentation.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.ItemType
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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

    // Cuenta actual, para saber si es dependiente y así poder ofrecer a su
    // padre como destinatario (antes solo se listaban los hijos, y una
    // cuenta dependiente nunca tiene hijos → el padre jamás aparecía).
    private val selfFlow = flow { emit(repository.getAccountById(accountId)) }

    @OptIn(ExperimentalCoroutinesApi::class)
    val otherAccounts: kotlinx.coroutines.flow.StateFlow<List<Account>> = selfFlow.flatMapLatest { self ->
        val childrenFlow = repository.getAccountsByParent(accountId)
        val parentId = self?.parentAccountId
        if (parentId != null) {
            val parentFlow = flow { emit(repository.getAccountById(parentId)) }
            combine(childrenFlow, parentFlow) { children, parent ->
                if (parent != null) listOf(parent) + children else children
            }
        } else {
            childrenFlow
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val externalRecipients = repository.getExternalRecipients(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _recipientName = MutableStateFlow("")
    val recipientName = _recipientName.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount = _amount.asStateFlow()

    private val _itemType = MutableStateFlow(ItemType.OTHER)
    val itemType = _itemType.asStateFlow()

    private var selectedReceiverId: Long? = null

    fun onRecipientNameChange(value: String) {
        _recipientName.value = value
        selectedReceiverId = null
    }

    fun onRecipientSelected(name: String, id: Long?) {
        _recipientName.value = name
        selectedReceiverId = id
    }

    fun onItemTypeChange(value: ItemType) { _itemType.value = value }
    fun onDescriptionChange(value: String) { _description.value = value }
    fun onAmountChange(value: String) {
        if (value.all { it.isDigit() }) {
            _amount.value = value
        }
    }

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
                recipientAccountName = _recipientName.value,
                receiverAccountId = selectedReceiverId,
                itemType = _itemType.value,
                itemName = _description.value
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