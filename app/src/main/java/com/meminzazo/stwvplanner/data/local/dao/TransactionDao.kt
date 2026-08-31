package com.meminzazo.stwvplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.meminzazo.stwvplanner.data.local.entity.TransactionEntity
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE (accountId = :accountId OR receiverAccountId = :accountId) AND NOT (source = 'GIFT' AND type = 'EARN' AND senderAccountId IS NOT NULL) ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @androidx.room.Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @androidx.room.Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE syncId = :syncId")
    suspend fun getTransactionBySyncId(syncId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT SUM(CASE WHEN receiverAccountId = :accountId THEN amount WHEN accountId = :accountId AND type = 'EARN' THEN amount ELSE -amount END) FROM transactions WHERE (accountId = :accountId OR receiverAccountId = :accountId) AND NOT (source = 'GIFT' AND type = 'EARN' AND senderAccountId IS NOT NULL)")
    fun getBalanceByAccount(accountId: Long): Flow<Int?>

    @Query("SELECT SUM(CASE WHEN receiverAccountId = :accountId THEN amount WHEN accountId = :accountId AND type = 'EARN' THEN amount ELSE -amount END) FROM transactions WHERE (accountId = :accountId OR receiverAccountId = :accountId) AND date >= :start AND date < :end AND NOT (source = 'GIFT' AND type = 'EARN' AND senderAccountId IS NOT NULL)")
    fun getBalanceByAccountInRange(accountId: Long, start: Long, end: Long): Flow<Int?>

    @Query("SELECT DISTINCT recipientAccountName FROM transactions WHERE accountId = :accountId AND receiverAccountId IS NULL AND recipientAccountName IS NOT NULL AND recipientAccountName != ''")
    fun getExternalRecipients(accountId: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId AND source = 'DAILY' AND date >= :startOfDay AND date < :endOfDay")
    suspend fun countDailyMissionsInDateRange(accountId: Long, startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :otherAccountId AND receiverAccountId = :accountId AND source = 'GIFT'")
    fun getVBucksReceivedFrom(accountId: Long, otherAccountId: Long): Flow<Int?>

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND receiverAccountId = :otherAccountId AND source = 'GIFT'")
    fun getVBucksSentTo(accountId: Long, otherAccountId: Long): Flow<Int?>

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :otherAccountId AND receiverAccountId = :accountId AND source = 'GIFT' AND date >= :start AND date < :end")
    fun getVBucksReceivedFromInRange(accountId: Long, otherAccountId: Long, start: Long, end: Long): Flow<Int?>

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND receiverAccountId = :otherAccountId AND source = 'GIFT' AND date >= :start AND date < :end")
    fun getVBucksSentToInRange(accountId: Long, otherAccountId: Long, start: Long, end: Long): Flow<Int?>

    @Query("SELECT * FROM transactions WHERE (accountId = :accountId OR receiverAccountId = :accountId) AND date >= :start AND date < :end AND NOT (source = 'GIFT' AND type = 'EARN' AND senderAccountId IS NOT NULL)")
    fun getTransactionsInRange(accountId: Long, start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE receiverAccountId = :accountId AND accountId = :senderId AND source = 'GIFT' AND NOT (source = 'GIFT' AND type = 'EARN' AND senderAccountId IS NOT NULL) ORDER BY date DESC")
    fun getGiftsReceivedFrom(accountId: Long, senderId: Long): Flow<List<TransactionEntity>>
}