package io.github.behnamrm.watertracker.drive

import io.github.behnamrm.watertracker.data.SettingsRepository
import io.github.behnamrm.watertracker.data.WaterRepository

class DriveBackupManager(
    private val repository: WaterRepository,
    private val settings: SettingsRepository,
    private val drive: DriveClient = DriveClient(),
) {
    suspend fun backup(token: String) {
        val json = repository.exportJson()
        val existing = drive.findBackup(token)
        drive.upload(token, existing?.id, json)
        settings.setLastBackupAt(System.currentTimeMillis())
    }

    /** Returns false when there is no backup on Drive yet. */
    suspend fun restore(token: String): Boolean {
        val file = drive.findBackup(token) ?: return false
        repository.importJson(drive.download(token, file.id))
        return true
    }
}
