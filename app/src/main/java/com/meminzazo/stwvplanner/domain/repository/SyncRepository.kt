package com.meminzazo.stwvplanner.domain.repository

import com.meminzazo.stwvplanner.data.local.entity.AccountEntity
import com.meminzazo.stwvplanner.data.local.entity.TransactionEntity

interface SyncRepository {
    suspend fun uploadAccount(userId: String, account: AccountEntity): Result<Unit>
    suspend fun uploadTransaction(userId: String, transaction: TransactionEntity): Result<Unit>
    
    suspend fun downloadAccounts(userId: String, since: Long): Result<List<AccountEntity>>
    suspend fun downloadTransactions(userId: String, since: Long): Result<List<TransactionEntity>>

    suspend fun syncAll(userId: String): Result<Unit>
    
    // Nuevas funciones de respaldo total
    suspend fun backupFullDatabase(userId: String): Result<Unit>
    suspend fun restoreFullDatabase(userId: String): Result<Unit>

    // Funciones para compartir por código
    suspend fun generateTransferCode(userId: String): Result<String>
    suspend fun restoreFromTransferCode(code: String): Result<Unit>
}
