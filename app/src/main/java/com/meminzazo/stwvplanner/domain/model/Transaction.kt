package com.meminzazo.stwvplanner.domain.model

data class Transaction(
    val id: Long = 0,
    val accountId: Long,
    val amount: Int,
    val type: TransactionType,
    val source: VBucksSource,
    val description: String,
    val date: Long,
    val recipientAccountName: String? = null
)
