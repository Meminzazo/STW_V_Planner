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
import kotlin.random.Random

class SyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) : SyncRepository {

    override suspend fun syncAll(userId: String): Result<Unit> {
        return try {
            var firstError: Exception? = null

            // 1. SUBIDA
            val unsyncedAccounts = accountDao.getUnsyncedAccounts()
            for (entity in unsyncedAccounts) {
                uploadAccount(userId, entity)
                    .onSuccess { accountDao.updateAccount(entity.copy(isSynced = true)) }
                    .onFailure { if (firstError == null) firstError = it as Exception }
            }

            val unsyncedTransactions = transactionDao.getUnsyncedTransactions()
            for (entity in unsyncedTransactions) {
                val updatedEntity = if (entity.accountSyncId == null) {
                    val acc = accountDao.getAccountById(entity.accountId)
                    entity.copy(accountSyncId = acc?.syncId)
                } else entity

                uploadTransaction(userId, updatedEntity)
                    .onSuccess { transactionDao.updateTransaction(updatedEntity.copy(isSynced = true)) }
                    .onFailure { if (firstError == null) firstError = it as Exception }
            }

            if (firstError != null) throw firstError!!

            // 2. DESCARGA
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

            // 3. REPARAR RELACIONES
            val allLocalAccounts = accountDao.getAllAccounts()
            for (acc in allLocalAccounts) {
                if (acc.parentSyncId != null && acc.parentAccountId == null) {
                    val parent = allLocalAccounts.find { it.syncId == acc.parentSyncId }
                    if (parent != null) {
                        accountDao.updateAccount(acc.copy(parentAccountId = parent.id))
                    }
                }
            }

            // 4. TRANSACCIONES
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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreFullDatabase(userId: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("backup")
                .document("latest")
                .get()
                .await()
            
            if (!snapshot.exists()) return Result.failure(Exception("No hay respaldo en la nube"))
            
            // Intentar leer datos paginados (chunked)
            val chunksCount = snapshot.getLong("totalChunks") ?: 0L
            val fullJson = if (chunksCount > 0) {
                val sb = StringBuilder()
                for (i in 0 until chunksCount.toInt()) {
                    val chunkDoc = firestore.collection("users")
                        .document(userId)
                        .collection("backup_chunks")
                        .document("chunk_$i")
                        .get()
                        .await()
                    sb.append(chunkDoc.getString("data") ?: "")
                }
                sb.toString()
            } else {
                // Compatibilidad con respaldo anterior (un solo documento)
                snapshot.getString("data") ?: return Result.failure(Exception("Datos de respaldo vacíos"))
            }

            restoreFromJson(fullJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun backupFullDatabase(userId: String): Result<Unit> {
        return try {
            val json = backupToJson()
            val chunkSize = 500_000 // ~500KB para estar seguros (Límite Firestore es 1MB)
            
            if (json.length > chunkSize) {
                val chunks = json.chunked(chunkSize)
                
                // 1. Guardar cada parte en una subcolección
                chunks.forEachIndexed { index, chunk ->
                    firestore.collection("users")
                        .document(userId)
                        .collection("backup_chunks")
                        .document("chunk_$index")
                        .set(mapOf("data" to chunk))
                        .await()
                }
                
                // 2. Actualizar el documento principal con el conteo
                firestore.collection("users")
                    .document(userId)
                    .collection("backup")
                    .document("latest")
                    .set(mapOf(
                        "totalChunks" to chunks.size,
                        "lastUpdated" to System.currentTimeMillis()
                    ))
                    .await()
            } else {
                // Si es pequeño, lo guardamos normal para ahorrar lecturas
                firestore.collection("users")
                    .document(userId)
                    .collection("backup")
                    .document("latest")
                    .set(mapOf(
                        "data" to json,
                        "totalChunks" to 0,
                        "lastUpdated" to System.currentTimeMillis()
                    ))
                    .await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateTransferCode(userId: String): Result<String> {
        return try {
            val code = (100000..999999).random().toString()
            val json = backupToJson()
            
            val data = mapOf(
                "data" to json,
                "createdAt" to System.currentTimeMillis()
            )
            
            firestore.collection("transfer_codes")
                .document(code)
                .set(data)
                .await()
                
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreFromTransferCode(code: String): Result<Unit> {
        return try {
            val snapshot = firestore.collection("transfer_codes")
                .document(code)
                .get()
                .await()
            
            if (!snapshot.exists()) return Result.failure(Exception("Código inválido"))
            
            val createdAt = snapshot.getLong("createdAt") ?: 0L
            val now = System.currentTimeMillis()
            val oneHourInMillis = 3600000L // 1 hora exacta
            
            if (now - createdAt > oneHourInMillis) {
                // El código ha expirado. Intentamos borrarlo para limpiar la nube
                firestore.collection("transfer_codes").document(code).delete()
                return Result.failure(Exception("El código ha expirado (validez de 1 hora)"))
            }
            
            val json = snapshot.getString("data") ?: return Result.failure(Exception("Datos corruptos"))
            
            // Borrar el código después de usarlo para que no pueda ser reutilizado
            firestore.collection("transfer_codes").document(code).delete().await()
            
            restoreFromJson(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun backupToJson(): String {
        val accounts = accountDao.getAllAccounts()
        val transactions = transactionDao.getAllTransactionsList()
        val backup = FullBackup(accounts, transactions)
        return com.google.gson.Gson().toJson(backup)
    }

    private suspend fun restoreFromJson(json: String): Result<Unit> {
        return try {
            val backup = com.google.gson.Gson().fromJson(json, FullBackup::class.java)
            accountDao.clearAllAccounts()
            transactionDao.clearAllTransactions()
            backup.accounts.forEach { accountDao.insertAccount(it) }
            backup.transactions.forEach { transactionDao.insertTransaction(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class FullBackup(
        val accounts: List<AccountEntity>,
        val transactions: List<TransactionEntity>
    )

    override suspend fun uploadAccount(userId: String, account: AccountEntity): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .document(account.syncId)
                .set(account.toFirestoreMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadTransaction(userId: String, transaction: TransactionEntity): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .document(transaction.syncId)
                .set(transaction.toFirestoreMap(), SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadAccounts(userId: String, since: Long): Result<List<AccountEntity>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .whereGreaterThan("lastUpdated", since)
                .get()
                .await()
            val accounts = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                AccountEntity(
                    syncId = doc.id,
                    name = data["name"] as? String ?: "",
                    isMain = data["isMain"] as? Boolean ?: false,
                    parentSyncId = data["parentSyncId"] as? String,
                    lastUpdated = data["lastUpdated"] as? Long ?: 0L,
                    isSynced = true
                )
            }
            Result.success(accounts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadTransactions(userId: String, since: Long): Result<List<TransactionEntity>> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .whereGreaterThan("lastUpdated", since)
                .get()
                .await()
            val transactions = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                TransactionEntity(
                    syncId = doc.id,
                    accountId = 0,
                    accountSyncId = data["accountSyncId"] as? String,
                    amount = (data["amount"] as? Long)?.toInt() ?: 0,
                    type = TransactionType.valueOf(data["type"] as? String ?: "EARN"),
                    source = VBucksSource.valueOf(data["source"] as? String ?: "OTHER"),
                    description = data["description"] as? String ?: "",
                    date = data["date"] as? Long ?: 0L,
                    recipientAccountName = data["recipientAccountName"] as? String,
                    lastUpdated = data["lastUpdated"] as? Long ?: 0L,
                    isSynced = true
                )
            }
            Result.success(transactions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun AccountEntity.toFirestoreMap() = mapOf(
        "syncId" to syncId,
        "name" to name,
        "isMain" to isMain,
        "parentSyncId" to parentSyncId,
        "lastUpdated" to lastUpdated
    )
    
    private fun TransactionEntity.toFirestoreMap() = mapOf(
        "syncId" to syncId,
        "accountSyncId" to accountSyncId,
        "amount" to amount,
        "type" to type.name,
        "source" to source.name,
        "description" to description,
        "date" to date,
        "recipientAccountName" to recipientAccountName,
        "itemType" to itemType?.name,
        "itemName" to itemName,
        "lastUpdated" to lastUpdated
    )
}
