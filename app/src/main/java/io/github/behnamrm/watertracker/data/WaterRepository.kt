package io.github.behnamrm.watertracker.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class WaterRepository(
    private val db: WaterDatabase,
    private val settings: SettingsRepository,
) {
    private val intakeDao = db.intakeDao()
    private val typeDao = db.drinkTypeDao()

    val drinkTypes: Flow<List<DrinkType>> = typeDao.observeActive()

    fun observeIntakes(fromMillis: Long, toMillis: Long): Flow<List<IntakeWithType>> =
        intakeDao.observeBetween(fromMillis, toMillis)

    suspend fun addIntake(drinkTypeId: Long, amountMl: Int, timestamp: Long = System.currentTimeMillis()): Long =
        intakeDao.insert(Intake(drinkTypeId = drinkTypeId, amountMl = amountMl, timestamp = timestamp))

    suspend fun deleteIntake(id: Long) = intakeDao.delete(id)

    /** Puts back an intake removed earlier (undo). */
    suspend fun restoreIntake(intake: Intake) {
        intakeDao.insert(intake)
    }

    suspend fun addDrinkType(name: String, emoji: String) {
        typeDao.insert(DrinkType(name = name, emoji = emoji, sortOrder = typeDao.maxSortOrder() + 1))
    }

    suspend fun updateDrinkType(type: DrinkType) = typeDao.update(type)

    suspend fun removeDrinkType(id: Long) = typeDao.archive(id)

    /** Serializes everything (drink types, intakes, settings) for a backup. */
    suspend fun exportJson(): String {
        val s = settings.current()
        val types = JSONArray()
        typeDao.getAll().forEach {
            types.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("emoji", it.emoji)
                    .put("sortOrder", it.sortOrder)
                    .put("favorite", it.favorite)
                    .put("archived", it.archived),
            )
        }
        val intakes = JSONArray()
        intakeDao.getAll().forEach {
            intakes.put(
                JSONObject()
                    .put("id", it.id)
                    .put("drinkTypeId", it.drinkTypeId)
                    .put("amountMl", it.amountMl)
                    .put("timestamp", it.timestamp),
            )
        }
        return JSONObject()
            .put("format", BACKUP_FORMAT)
            .put("version", BACKUP_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put(
                "settings",
                JSONObject().put("dailyGoalMl", s.dailyGoalMl).put("glassSizeMl", s.glassSizeMl),
            )
            .put("drinkTypes", types)
            .put("intakes", intakes)
            .toString()
    }

    /** Replaces all local data with the content of a backup made by [exportJson]. */
    suspend fun importJson(json: String) {
        val root: JSONObject
        val types: List<DrinkType>
        val intakes: List<Intake>
        try {
            root = JSONObject(json)
            require(root.optString("format") == BACKUP_FORMAT) { "Not a Water Tracker backup" }
            val typeArray = root.getJSONArray("drinkTypes")
            types = (0 until typeArray.length()).map { i ->
                val o = typeArray.getJSONObject(i)
                DrinkType(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    emoji = o.getString("emoji"),
                    sortOrder = o.optInt("sortOrder", i),
                    favorite = o.optBoolean("favorite", false),
                    archived = o.optBoolean("archived", false),
                )
            }
            val intakeArray = root.getJSONArray("intakes")
            intakes = (0 until intakeArray.length()).map { i ->
                val o = intakeArray.getJSONObject(i)
                Intake(
                    id = o.getLong("id"),
                    drinkTypeId = o.getLong("drinkTypeId"),
                    amountMl = o.getInt("amountMl"),
                    timestamp = o.getLong("timestamp"),
                )
            }
        } catch (e: JSONException) {
            throw IllegalArgumentException("Backup file is damaged", e)
        }
        val typeIds = types.map { it.id }.toSet()
        require(intakes.all { it.drinkTypeId in typeIds }) { "Backup file is damaged" }

        db.withTransaction {
            intakeDao.deleteAll()
            typeDao.deleteAll()
            typeDao.insertAll(types)
            intakeDao.insertAll(intakes)
        }
        root.optJSONObject("settings")?.let { s ->
            s.optInt("dailyGoalMl").takeIf { it > 0 }?.let { settings.setDailyGoal(it) }
            s.optInt("glassSizeMl").takeIf { it > 0 }?.let { settings.setGlassSize(it) }
        }
    }

    private companion object {
        const val BACKUP_FORMAT = "water-tracker-backup"
        const val BACKUP_VERSION = 1
    }
}
