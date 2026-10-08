package com.meminzazo.stwvplanner.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.meminzazo.stwvplanner.data.local.VBucksDatabase
import com.meminzazo.stwvplanner.data.local.dao.AccountDao
import com.meminzazo.stwvplanner.data.local.dao.TransactionDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.meminzazo.stwvplanner.data.local.entity.AccountEntity
import com.meminzazo.stwvplanner.data.local.entity.TransactionEntity
import com.meminzazo.stwvplanner.domain.model.TransactionType
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.repository.SyncRepository
import com.meminzazo.stwvplanner.domain.repository.SharedViewRepository
import kotlinx.coroutines.tasks.await
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val db: VBucksDatabase,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val sharedViewRepository: SharedViewRepository
) : SyncRepository {

    override suspend fun syncAll(userId: String): Result<Unit> {
        // En v3.0 el syncAll automático está deshabilitado para evitar spam de cuota.
        return Result.success(Unit)
    }

    override suspend fun restoreFullDatabase(userId: String): Result<Unit> {
        return try {
            val metadata = firestore.collection("users").document(userId).collection("backup").document("latest").get().await()
            if (!metadata.exists()) return Result.failure(Exception("Sin respaldo"))
            val backupId = metadata.getString("backupId")
            val chunksCount = (metadata.getLong("totalChunks") ?: 0L).toInt()
            val fullJson = if (chunksCount > 0) {
                buildString {
                    for (i in 0 until chunksCount) {
                        val chunkId = if (backupId != null) "${backupId}_$i" else "chunk_$i"
                        val chunk = firestore.collection("users").document(userId)
                            .collection("backup_chunks").document(chunkId).get().await()
                        if (!chunk.exists()) throw IllegalStateException("Falta el fragmento $i")
                        append(chunk.getString("data") ?: throw IllegalStateException("Fragmento vacío"))
                    }
                }
            } else {
                metadata.getString("data") ?: return Result.failure(Exception("Vacío"))
            }
            val fullBytes = fullJson.toByteArray(StandardCharsets.UTF_8)
            val expectedLength = metadata.getLong("byteLength")
            val expectedHash = metadata.getString("sha256")
            if (expectedLength != null && expectedLength != fullBytes.size.toLong()) {
                return Result.failure(Exception("Respaldo incompleto o alterado"))
            }
            if (expectedHash != null && expectedHash != sha256(fullBytes)) {
                return Result.failure(Exception("Respaldo incompleto o alterado"))
            }
            restoreFromJson(fullJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun backupFullDatabase(userId: String): Result<Unit> {
        return try {
            val json = backupToJson()
            val bytes = json.toByteArray(StandardCharsets.UTF_8)
            if (bytes.size > MAX_BACKUP_BYTES) {
                return Result.failure(Exception("Respaldo demasiado grande (>2MB). Elimina registros antiguos."))
            }
            val backupId = UUID.randomUUID().toString()
            val backupRef = firestore.collection("users").document(userId).collection("backup")
            val chunkRef = firestore.collection("users").document(userId).collection("backup_chunks")
            val previousBackupId = backupRef.document("latest").get().await()
                .getString("backupId")
            val chunks = json.chunked(CHUNK_CHAR_SIZE)
            val metadata = mapOf(
                "backupId" to backupId,
                "totalChunks" to if (chunks.size == 1) 0 else chunks.size,
                "byteLength" to bytes.size,
                "sha256" to sha256(bytes),
                "lastUpdated" to System.currentTimeMillis()
            )
            if (chunks.size == 1) {
                backupRef.document("staging_$backupId").set(metadata + ("data" to json)).await()
            } else {
                chunks.forEachIndexed { i, chunk ->
                    chunkRef.document("${backupId}_$i").set(mapOf("backupId" to backupId, "index" to i, "data" to chunk)).await()
                }
                backupRef.document("staging_$backupId").set(metadata).await()
            }
            backupRef.document("latest").set(metadata + if (chunks.size == 1) mapOf("data" to json) else emptyMap()).await()
            sharedViewRepository.refreshOwnedSharedViews(userId)
                .onFailure { Log.w("SyncRepository", "Respaldo guardado, pero no se actualizaron las vistas compartidas", it) }
            backupRef.document("staging_$backupId").delete().await()
            if (previousBackupId != null && previousBackupId != backupId) {
                val oldChunks = chunkRef.whereEqualTo("backupId", previousBackupId).get().await()
                oldChunks.documents.forEach { it.reference.delete().await() }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_BACKUP_BYTES = 2_000_000
        const val CHUNK_CHAR_SIZE = 450_000
    }

    override suspend fun generateTransferCode(userId: String): Result<String> {
        return try {
            // Códigos puramente numéricos de 10 dígitos
            val charPool = "0123456789"
            val code = (1..10).map { charPool.random() }.joinToString("")

            val json = backupToJson()
            // Protección de cuota: Límite de tamaño en transferencia también (también forzado en firestore.rules)
            if (json.length > 2_000_000) {
                return Result.failure(Exception("Datos demasiado grandes para transferir"))
            }

            firestore.collection("transfer_codes").document(code)
                .set(mapOf("data" to json, "createdAt" to System.currentTimeMillis(), "createdBy" to userId))
                .await()
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreFromTransferCode(code: String): Result<Unit> {
        Log.d("SyncRepository", "Iniciando restoreFromTransferCode para el código: $code")
        Log.d("SyncRepository", "Estado Auth: firebaseUser=${FirebaseAuth.getInstance().currentUser?.uid}")
        return try {
            val snapshot = firestore.collection("transfer_codes").document(code).get().await()
            if (!snapshot.exists()) {
                Log.e("SyncRepository", "Código no encontrado en Firestore: $code")
                return Result.failure(Exception("Código no encontrado"))
            }
            val createdAt = snapshot.getLong("createdAt") ?: 0L
            Log.d("SyncRepository", "Código encontrado. Creado en: $createdAt")

            // Caducidad de 1 hora para protección
            if (System.currentTimeMillis() - createdAt > 3600000L) {
                Log.w("SyncRepository", "El código $code ha expirado")
                firestore.collection("transfer_codes").document(code).delete()
                return Result.failure(Exception("Código expirado (1h)"))
            }
            val json = snapshot.getString("data") ?: run {
                Log.e("SyncRepository", "El código $code existe pero no tiene datos")
                return Result.failure(Exception("Datos vacíos"))
            }
            Log.d("SyncRepository", "JSON recibido de Firestore (longitud: ${json.length})")

            // Opcional: borrar el código tras su uso para evitar spam
            firestore.collection("transfer_codes").document(code).delete().await()
            Log.d("SyncRepository", "Código $code eliminado tras recuperación exitosa")

            restoreFromJson(json)
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error en restoreFromTransferCode: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getFullDatabaseJson(): Result<String> {
        return try {
            Result.success(backupToJson())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreDatabaseFromJson(json: String): Result<Unit> {
        return restoreFromJson(json)
    }

    private suspend fun backupToJson(): String {
        val accounts = accountDao.getAllAccounts()
        val transactions = transactionDao.getAllTransactionsList()
        return com.google.gson.Gson().toJson(FullBackup(accounts, transactions))
    }

    private suspend fun restoreFromJson(json: String): Result<Unit> {
        Log.d("SyncRepository", "Iniciando restoreFromJson")
        return try {
            val backup = Gson().fromJson(json, FullBackup::class.java)
            validateBackup(backup)
            Log.d("SyncRepository", "Deserealización exitosa: ${backup.accounts.size} cuentas, ${backup.transactions.size} transacciones")
            
            // withTransaction: si algo falla a medio proceso (archivo corrupto, entidad inválida),
            // se revierte todo -> nunca se queda la BD vacía o a medias.
            db.withTransaction {
                Log.d("SyncRepository", "Iniciando transacción de base de datos")
                accountDao.clearAllAccounts()
                transactionDao.clearAllTransactions()
                backup.accounts.forEach { accountDao.insertAccount(it) }
                backup.transactions.forEach { transactionDao.insertTransaction(it) }
                Log.d("SyncRepository", "Transacción de base de datos completada")
            }
            Log.d("SyncRepository", "Restauración finalizada con éxito")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error en restoreFromJson: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun validateBackup(backup: FullBackup) {
        require(backup.accounts.isNotEmpty() || backup.transactions.isEmpty()) { "Respaldo sin cuentas" }
        val accountIds = backup.accounts.map { it.id }.toSet()
        val accountSyncIds = backup.accounts.map { it.syncId }.toSet()
        require(accountSyncIds.size == backup.accounts.size) { "syncId de cuenta duplicado" }
        require(backup.accounts.none { it.name.isBlank() || it.syncId.isBlank() }) { "Cuenta inválida" }
        require(backup.transactions.all { transaction ->
            transaction.syncId.isNotBlank() &&
                (transaction.accountId in accountIds || transaction.accountSyncId in accountSyncIds) &&
                transaction.amount >= 0 &&
                transaction.description.isNotBlank()
        }) { "Transacción inválida o sin cuenta asociada" }
        require(backup.transactions.map { it.syncId }.toSet().size == backup.transactions.size) {
            "syncId de transacción duplicado"
        }
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