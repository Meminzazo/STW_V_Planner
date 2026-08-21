package com.meminzazo.stwvplanner.domain.usecase

import com.meminzazo.stwvplanner.domain.model.Transaction
import com.meminzazo.stwvplanner.domain.model.VBucksSource
import com.meminzazo.stwvplanner.domain.repository.VBucksRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: VBucksRepository
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> {
        if (transaction.source == VBucksSource.DAILY) {
            val count = repository.countDailyMissionsInDate(transaction.accountId, transaction.date)
            if (count > 0) {
                return Result.failure(Exception("Ya has registrado una misión diaria hoy."))
            }
        }
        
        return try {
            val id = repository.insertTransaction(transaction)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
