package com.meminzazo.stwvplanner.domain.model

data class SharingPayload(
    val account: Account,
    val transactions: List<Transaction>
)
