package com.echo.inbox.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.echo.inbox.EchoInboxApplication
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

class RetentionCleanupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as EchoInboxApplication).container
        val retentionDays = container.settingsRepository.current().retentionDays
        if (retentionDays <= 0) return Result.success()

        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        return try {
            container.notificationRepository.deleteOlderThan(cutoff)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
