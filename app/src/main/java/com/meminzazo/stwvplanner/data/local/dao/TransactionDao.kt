package com.meminzazo.stwvplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.meminzazo.stwvplanner.data.local.entity.TransactionEntity
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("SELECT SUM(CASE WHEN type = 'EARN' THEN amount ELSE -amount END) FROM transactions WHERE accountId = :accountId")
    fun getBalanceByAccount(accountId: Long): Flow<Int?>

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId AND source = 'DAILY' AND date >= :startOfDay AND date < :endOfDay")
    suspend fun countDailyMissionsInDateRange(accountId: Long, startOfDay: Long, endOfDay: Long): Int
}
