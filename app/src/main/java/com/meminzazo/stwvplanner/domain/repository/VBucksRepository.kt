package com.meminzazo.stwvplanner.domain.repository

import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface VBucksRepository {
    fun getAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun insertAccount(account: Account): Long
    suspend fun deleteAccount(id: Long)

    fun getTransactions(accountId: Long? = null): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction): Long
    fun getBalance(accountId: Long): Flow<Int>
    suspend fun countDailyMissionsInDate(accountId: Long, timestamp: Long): Int
}
