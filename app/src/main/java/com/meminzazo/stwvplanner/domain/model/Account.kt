package com.meminzazo.stwvplanner.domain.model

data class Account(
    val id: Long = 0,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val isMain: Boolean = false,
    val balance: Int = 0,
    val parentAccountId: Long? = null
)
