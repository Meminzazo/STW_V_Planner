package com.meminzazo.stwvplanner.data.repository

import com.meminzazo.stwvplanner.data.local.dao.AccountDao
import com.meminzazo.stwvplanner.data.local.dao.TransactionDao
import com.meminzazo.stwvplanner.data.local.entity.AccountEntity
import com.meminzazo.stwvplanner.data.mapper.toDomain
import com.meminzazo.stwvplanner.data.mapper.toEntity
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

class VBucksRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val scheduleSyncUseCase: javax.inject.Provider<com.meminzazo.stwvplanner.domain.usecase.ScheduleSyncUseCase>
) : VBucksRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getMainAccounts(): Flow<List<Account>> {
        return accountDao.getMainAccounts().flatMapLatest { entities ->
            mapEntitiesToDomain(entities)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAccountsByParent(parentId: Long): Flow<List<Account>> {
        return accountDao.getAccountsByParent(parentId).flatMapLatest { entities ->
            mapEntitiesToDomain(entities)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAccounts(): Flow<List<Account>> {
        // Por defecto devolvemos todas, pero el mapper se encargará de isMain
        return accountDao.getMainAccounts().flatMapLatest { entities ->
            mapEntitiesToDomain(entities)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun mapEntitiesToDomain(entities: List<AccountEntity>): Flow<List<Account>> {
        if (entities.isEmpty()) return flowOf(emptyList())
        val accountFlows = entities.map { entity ->
            transactionDao.getBalanceByAccount(entity.id).map { balance ->
                entity.toDomain(balance ?: 0)
            }
        }
        return combine(accountFlows) { it.toList() }
    }

    override suspend fun getAccountById(id: Long): Account? {
        // Implementación simplificada sin balance para el getById (o se puede añadir)
        return accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun insertAccount(account: Account): Long {
        val entity = account.toEntity().copy(isSynced = false, lastUpdated = System.currentTimeMillis())
        val id = accountDao.insertAccount(entity)
        scheduleSyncUseCase.get().invoke()
        return id
    }

    override suspend fun updateAccount(account: Account) {
        val entity = account.toEntity().copy(isSynced = false, lastUpdated = System.currentTimeMillis())
        accountDao.updateAccount(entity)
        scheduleSyncUseCase.get().invoke()
    }

    override suspend fun deleteAccount(id: Long) {
        accountDao.deleteAccount(id)
        scheduleSyncUseCase.get().invoke()
    }

    override fun getTransactions(accountId: Long?): Flow<List<Transaction>> {
        val flow = if (accountId != null) {
            transactionDao.getTransactionsByAccount(accountId)
        } else {
            transactionDao.getAllTransactions()
        }
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val account = accountDao.getAccountById(transaction.accountId)
        val entity = transaction.toEntity().copy(
            accountSyncId = account?.syncId,
            isSynced = false, 
            lastUpdated = System.currentTimeMillis()
        )
        val transactionId = transactionDao.insertTransaction(entity)

        // Lógica de doble inserción para regalos entre cuentas locales
        if (transaction.source == com.meminzazo.stwvplanner.domain.model.VBucksSource.GIFT) {
            if (transaction.type == com.meminzazo.stwvplanner.domain.model.TransactionType.SPEND && transaction.receiverAccountId != null) {
                // Si yo regalo, el otro recibe
                val receivingTransaction = transaction.copy(
                    id = 0,
                    accountId = transaction.receiverAccountId,
                    type = com.meminzazo.stwvplanner.domain.model.TransactionType.EARN,
                    senderAccountId = transaction.accountId,
                    receiverAccountId = null
                )
                transactionDao.insertTransaction(receivingTransaction.toEntity().copy(isSynced = false, lastUpdated = System.currentTimeMillis()))
            }
        }
        
        scheduleSyncUseCase.get().invoke()

        return transactionId
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override fun getBalance(accountId: Long): Flow<Int> {
        return transactionDao.getBalanceByAccount(accountId).map { it ?: 0 }
    }

    override suspend fun countDailyMissionsInDate(accountId: Long, timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        return transactionDao.countDailyMissionsInDateRange(accountId, startOfDay, endOfDay)
    }

    override fun getVBucksReceivedFrom(accountId: Long, otherAccountId: Long): Flow<Int> {
        return transactionDao.getVBucksReceivedFrom(accountId, otherAccountId).map { it ?: 0 }
    }

    override fun getVBucksSentTo(accountId: Long, otherAccountId: Long): Flow<Int> {
        return transactionDao.getVBucksSentTo(accountId, otherAccountId).map { it ?: 0 }
    }

    override fun getVBucksReceivedFromInRange(accountId: Long, otherAccountId: Long, start: Long, end: Long): Flow<Int> {
        return transactionDao.getVBucksReceivedFromInRange(accountId, otherAccountId, start, end).map { it ?: 0 }
    }

    override fun getVBucksSentToInRange(accountId: Long, otherAccountId: Long, start: Long, end: Long): Flow<Int> {
        return transactionDao.getVBucksSentToInRange(accountId, otherAccountId, start, end).map { it ?: 0 }
    }

    override fun getTransactionsInRange(accountId: Long, start: Long, end: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsInRange(accountId, start, end).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getGiftsReceivedFrom(accountId: Long, senderId: Long): Flow<List<Transaction>> {
        return transactionDao.getGiftsReceivedFrom(accountId, senderId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBalanceInRange(accountId: Long, start: Long, end: Long): Flow<Int> {
        return transactionDao.getBalanceByAccountInRange(accountId, start, end).map { it ?: 0 }
    }

    override fun getExternalRecipients(accountId: Long): Flow<List<String>> {
        return transactionDao.getExternalRecipients(accountId)
    }
}