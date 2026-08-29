package com.antigravity.unlockly.worker

import android.content.Context
import androidx.work.*
import com.antigravity.unlockly.UnlocklyApplication
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MidnightResetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = UnlocklyApplication.instance
            app.walletManager.performMidnightReset()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "midnight_reset_worker"

        fun scheduleDailyReset(context: Context) {
            val now = Calendar.getInstance()
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val initialDelay = nextMidnight.timeInMillis - now.timeInMillis

            val workRequest = PeriodicWorkRequestBuilder<MidnightResetWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
