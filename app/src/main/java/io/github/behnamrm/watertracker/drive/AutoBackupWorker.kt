package io.github.behnamrm.watertracker.drive

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.behnamrm.watertracker.WaterApp
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Backs up to Google Drive once a day, as long as Drive access was granted before. */
class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as WaterApp).container
        val auth = try {
            container.driveAuthorizer.authorize()
        } catch (e: Exception) {
            return Result.retry()
        }
        // Access needs the user's consent again; nothing we can do without UI.
        val token = auth.accessToken
        if (auth.hasResolution() || token == null) return Result.success()
        return try {
            container.driveBackup.backup(token)
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "drive_auto_backup"

        fun schedule(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
