package com.meminzazo.stwvplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.meminzazo.stwvplanner.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE parentAccountId IS NULL")
    fun getMainAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE parentAccountId = :parentId")
    fun getAccountsByParent(parentId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE syncId = :syncId")
    suspend fun getAccountBySyncId(syncId: String): AccountEntity?

    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE isSynced = 0")
    suspend fun getUnsyncedAccounts(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @androidx.room.Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun clearAllAccounts()

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: Long)
}
