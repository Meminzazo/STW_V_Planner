package com.meminzazo.stwvplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.meminzazo.stwvplanner.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE parentAccountId IS NULL AND isDeleted = 0")
    fun getMainAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE parentAccountId IS NULL AND isDeleted = 1")
    fun getDeletedMainAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE parentAccountId = :parentId AND isDeleted = 0")
    fun getAccountsByParent(parentId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE parentAccountId = :parentId AND isDeleted = 1")
    fun getDeletedAccountsByParent(parentId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id AND isDeleted = 0")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE syncId = :syncId AND isDeleted = 0")
    suspend fun getAccountBySyncId(syncId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE isDeleted = 0")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE isSynced = 0")
    suspend fun getUnsyncedAccounts(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @androidx.room.Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()

    @Query("UPDATE accounts SET isDeleted = 1, isSynced = 0, lastUpdated = :timestamp WHERE id = :id")
    suspend fun softDeleteAccount(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE accounts SET isDeleted = 0, isSynced = 0, lastUpdated = :timestamp WHERE id = :id")
    suspend fun restoreAccount(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun hardDeleteAccount(id: Long)
}
