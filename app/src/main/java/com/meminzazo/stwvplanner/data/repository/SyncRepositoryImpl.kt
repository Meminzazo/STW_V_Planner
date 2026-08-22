package com.meminzazo.stwvplanner.data.repository

import com.meminzazo.stwvplanner.data.local.dao.AccountDao
import com.meminzazo.stwvplanner.data.local.dao.TransactionDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.meminzazo.stwvplanner.data.local.entity.AccountEntity
import com.meminzazo.stwvplanner.data.local.entity.TransactionEntity
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.repository.SyncRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) : SyncRepository {

    override suspend fun syncAll(userId: String): Result<Unit> {
        return try {
            var firstError: Exception? = null

            // 1. SUBIDA (Limitada a 50 items para evitar abuso de cuota)
            val unsyncedAccounts = accountDao.getUnsyncedAccounts().take(10)
            for (entity in unsyncedAccounts) {
                uploadAccount(userId, entity)
                    .onSuccess { accountDao.updateAccount(entity.copy(isSynced = true)) }
                    .onFailure { if (firstError == null) firstError = it as Exception }
            }

            val unsyncedTransactions = transactionDao.getUnsyncedTransactions().take(50)
            for (entity in unsyncedTransactions) {
                val updatedEntity = if (entity.accountSyncId == null) {
                    val acc = accountDao.getAccountById(entity.accountId)
                    entity.copy(accountSyncId = acc?.syncId)
                } else entity

                uploadTransaction(userId, updatedEntity)
                    .onSuccess { transactionDao.updateTransaction(updatedEntity.copy(isSynced = true)) }
                    .onFailure { if (firstError == null) firstError = it as Exception }
            }

            // 2. DESCARGA E INTEGRACIÓN
            downloadAccounts(userId, 0).onSuccess { remoteAccounts ->
                for (remote in remoteAccounts) {
                    val local = accountDao.getAccountBySyncId(remote.syncId)
                    if (local == null) {
                        accountDao.insertAccount(remote.copy(id = 0, isSynced = true))
                    } else if (remote.lastUpdated > local.lastUpdated) {
                        accountDao.updateAccount(remote.copy(id = local.id, isSynced = true))
                    }
                }
            }

            // Reparar relaciones localmente
            val allLocalAccounts = accountDao.getAllAccounts()
            for (acc in allLocalAccounts) {
                if (acc.parentSyncId != null && acc.parentAccountId == null) {
                    val parent = allLocalAccounts.find { it.syncId == acc.parentSyncId }
                    if (parent != null) {
                        accountDao.updateAccount(acc.copy(parentAccountId = parent.id))
                    }
                }
            }

            downloadTransactions(userId, 0).onSuccess { remoteTransactions ->
                for (remote in remoteTransactions) {
                    val local = transactionDao.getTransactionBySyncId(remote.syncId)
                    val parentAccount = accountDao.getAccountBySyncId(remote.accountSyncId ?: "")
                    if (parentAccount != null) {
                        if (local == null) {
                            transactionDao.insertTransaction(remote.copy(id = 0, accountId = parentAccount.id, isSynced = true))
                        } else if (remote.lastUpdated > local.lastUpdated) {
                            transactionDao.updateTransaction(remote.copy(id = local.id, accountId = parentAccount.id, isSynced = true))
                        }
                    }
                }
            }

            if (firstError != null) Result.failure(firstError!!) else Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreFullDatabase(userId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("users").document(userId).collection("backup").document("latest").get().await()
            if (!snapshot.exists()) return Result.failure(Exception("Sin respaldo"))
            
            val chunksCount = snapshot.getLong("totalChunks") ?: 0L
            val fullJson = if (chunksCount > 0) {
                val sb = StringBuilder()
                for (i in 0 until chunksCount.toInt()) {
                    val chunk = firestore.collection("users").document(userId).collection("backup_chunks").document("chunk_$i").get().await()
                    sb.append(chunk.getString("data") ?: "")
                }
                sb.toString()
            } else {
                snapshot.getString("data") ?: return Result.failure(Exception("Vacío"))
            }
            restoreFromJson(fullJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun backupFullDatabase(userId: String): Result<Unit> {
        return try {
            val json = backupToJson()
            val chunkSize = 500_000
            if (json.length > chunkSize) {
                val chunks = json.chunked(chunkSize)
                chunks.forEachIndexed { i, c ->
                    firestore.collection("users").document(userId).collection("backup_chunks").document("chunk_$i").set(mapOf("data" to c)).await()
                }
                firestore.collection("users").document(userId).collection("backup").document("latest").set(mapOf("totalChunks" to chunks.size, "lastUpdated" to System.currentTimeMillis())).await()
            } else {
                firestore.collection("users").document(userId).collection("backup").document("latest").set(mapOf("data" to json, "totalChunks" to 0, "lastUpdated" to System.currentTimeMillis())).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateTransferCode(userId: String): Result<String> {
        return try {
            // Código alfanumérico de 8 caracteres (Seguridad Industrial)
            val charPool = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val code = (1..8).map { charPool.random() }.joinToString("")
            
            val json = backupToJson()
            firestore.collection("transfer_codes").document(code).set(mapOf("data" to json, "createdAt" to System.currentTimeMillis())).await()
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreFromTransferCode(code: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("transfer_codes").document(code).get().await()
            if (!snapshot.exists()) return Result.failure(Exception("Código no encontrado"))
            val createdAt = snapshot.getLong("createdAt") ?: 0L
            if (System.currentTimeMillis() - createdAt > 3600000L) {
                firestore.collection("transfer_codes").document(code).delete()
                return Result.failure(Exception("Código expirado (1h)"))
            }
            val json = snapshot.getString("data") ?: return Result.failure(Exception("Datos vacíos"))
            firestore.collection("transfer_codes").document(code).delete().await()
            restoreFromJson(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun backupToJson(): String {
        val accounts = accountDao.getAllAccounts()
        val transactions = transactionDao.getAllTransactionsList()
        return com.google.gson.Gson().toJson(FullBackup(accounts, transactions))
    }

    private suspend fun restoreFromJson(json: String): Result<Unit> {
        return try {
            val backup = com.google.gson.Gson().fromJson(json, FullBackup::class.java)
            accountDao.clearAllAccounts()
            transactionDao.clearAllTransactions()
            backup.accounts.forEach { accountDao.insertAccount(it) }
            backup.transactions.forEach { transactionDao.insertTransaction(it) }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private data class FullBackup(val accounts: List<AccountEntity>, val transactions: List<TransactionEntity>)

    override suspend fun uploadAccount(userId: String, account: AccountEntity): Result<Unit> {
        return try {
            firestore.collection("users").document(userId).collection("accounts").document(account.syncId).set(account.toFirestoreMap(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun uploadTransaction(userId: String, transaction: TransactionEntity): Result<Unit> {
        return try {
            firestore.collection("users").document(userId).collection("transactions").document(transaction.syncId).set(transaction.toFirestoreMap(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun downloadAccounts(userId: String, since: Long): Result<List<AccountEntity>> {
        return try {
            val snapshot = firestore.collection("users").document(userId).collection("accounts").whereGreaterThan("lastUpdated", since).get().await()
            val accounts = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                AccountEntity(syncId = doc.id, name = data["name"] as? String ?: "", isMain = data["isMain"] as? Boolean ?: false, parentSyncId = data["parentSyncId"] as? String, lastUpdated = data["lastUpdated"] as? Long ?: 0L, isSynced = true)
            }
            Result.success(accounts)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun downloadTransactions(userId: String, since: Long): Result<List<TransactionEntity>> {
        return try {
            val snapshot = firestore.collection("users").document(userId).collection("transactions").whereGreaterThan("lastUpdated", since).get().await()
            val transactions = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                TransactionEntity(syncId = doc.id, accountId = 0, accountSyncId = data["accountSyncId"] as? String, amount = (data["amount"] as? Long)?.toInt() ?: 0, type = TransactionType.valueOf(data["type"] as? String ?: "EARN"), source = VBucksSource.valueOf(data["source"] as? String ?: "OTHER"), description = data["description"] as? String ?: "", date = data["date"] as? Long ?: 0L, recipientAccountName = data["recipientAccountName"] as? String, lastUpdated = data["lastUpdated"] as? Long ?: 0L, isSynced = true)
            }
            Result.success(transactions)
        } catch (e: Exception) { Result.failure(e) }
    }
    
    private fun AccountEntity.toFirestoreMap() = mapOf("syncId" to syncId, "name" to name, "isMain" to isMain, "parentSyncId" to parentSyncId, "lastUpdated" to lastUpdated)
    private fun TransactionEntity.toFirestoreMap() = mapOf("syncId" to syncId, "accountSyncId" to accountSyncId, "amount" to amount, "type" to type.name, "source" to source.name, "description" to description, "date" to date, "recipientAccountName" to recipientAccountName, "itemType" to itemType?.name, "itemName" to itemName, "lastUpdated" to lastUpdated)
}
