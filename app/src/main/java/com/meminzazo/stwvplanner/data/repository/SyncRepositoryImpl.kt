package com.meminzazo.stwvplanner.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.meminzazo.stwvplanner.data.local.entity.AccountEntity
import com.meminzazo.stwvplanner.data.local.entity.TransactionEntity
import com.meminzazo.stwvplanner.domain.repository.SyncRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SyncRepository {

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
            // Map snapshot to entities (simplified for now)
            Result.success(emptyList())
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
            Result.success(emptyList())
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
