package com.meminzazo.stwvplanner.data.repository

import com.meminzazo.stwvplanner.data.local.dao.AccountDao
import com.meminzazo.stwvplanner.data.local.dao.TransactionDao
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
    private val transactionDao: TransactionDao
) : VBucksRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().flatMapLatest { entities ->
            if (entities.isEmpty()) return@flatMapLatest flowOf(emptyList<Account>())
            
            val accountFlows = entities.map { entity ->
                transactionDao.getBalanceByAccount(entity.id).map { balance ->
                    entity.toDomain(balance ?: 0)
                }
            }
            combine(accountFlows) { it.toList() }
        }
    }

    override suspend fun getAccountById(id: Long): Account? {
        // Implementación simplificada sin balance para el getById (o se puede añadir)
        return accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun insertAccount(account: Account): Long {
        return accountDao.insertAccount(account.toEntity())
    }

    override suspend fun deleteAccount(id: Long) {
        accountDao.deleteAccount(id)
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
        return transactionDao.insertTransaction(transaction.toEntity())
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
}
