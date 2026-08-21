package com.meminzazo.stwvplanner.domain.usecase

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.meminzazo.stwvplanner.data.worker.DailyReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ScheduleReminderUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18) // 6 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        var delay = calendar.timeInMillis - System.currentTimeMillis()
        if (delay < 0) {
            delay += TimeUnit.DAYS.toMillis(1)
        }

        val workRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
