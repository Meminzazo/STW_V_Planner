package com.meminzazo.stwvplanner.domain.usecase

import androidx.work.*
import com.meminzazo.stwvplanner.data.worker.SyncWorker
import javax.inject.Inject

class ScheduleSyncUseCase @Inject constructor(
    private val workManager: WorkManager
) {
    operator fun invoke() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "SyncWork",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }
}
