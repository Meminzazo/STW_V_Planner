package com.meminzazo.stwvplanner.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.meminzazo.stwvplanner.domain.repository.AuthRepository
import com.meminzazo.stwvplanner.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val user = authRepository.currentUser.first()
        return if (user != null) {
            val result = syncRepository.syncAll(user.id)
            if (result.isSuccess) Result.success() else Result.retry()
        } else {
            Result.failure()
        }
    }
}
