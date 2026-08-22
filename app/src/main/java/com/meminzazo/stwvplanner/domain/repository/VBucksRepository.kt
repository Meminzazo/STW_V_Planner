package com.meminzazo.stwvplanner.domain.repository

import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface VBucksRepository {
    fun getMainAccounts(): Flow<List<Account>>
    fun getAccountsByParent(parentId: Long): Flow<List<Account>>
    fun getAccounts(): Flow<List<Account>> // Mantenemos para compatibilidad si es necesario, pero filtraremos
    suspend fun getAccountById(id: Long): Account?
    suspend fun insertAccount(account: Account): Long
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: Long)

    fun getTransactions(accountId: Long? = null): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    fun getBalance(accountId: Long): Flow<Int>
    suspend fun countDailyMissionsInDate(accountId: Long, timestamp: Long): Int

    fun getVBucksReceivedFrom(accountId: Long, otherAccountId: Long): Flow<Int>
    fun getVBucksSentTo(accountId: Long, otherAccountId: Long): Flow<Int>
    fun getVBucksReceivedFromInRange(accountId: Long, otherAccountId: Long, start: Long, end: Long): Flow<Int>
    fun getVBucksSentToInRange(accountId: Long, otherAccountId: Long, start: Long, end: Long): Flow<Int>
    fun getGiftsReceivedFrom(accountId: Long, senderId: Long): Flow<List<Transaction>>
    fun getTransactionsInRange(accountId: Long, start: Long, end: Long): Flow<List<Transaction>>
    fun getBalanceInRange(accountId: Long, start: Long, end: Long): Flow<Int>
    fun getExternalRecipients(accountId: Long): Flow<List<String>>
}