package io.github.behnamrm.watertracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeDao {
    @Query(
        """
        SELECT i.id, i.drinkTypeId, i.amountMl, i.timestamp, t.name AS typeName, t.emoji AS typeEmoji
        FROM intakes i JOIN drink_types t ON t.id = i.drinkTypeId
        WHERE i.timestamp >= :fromMillis AND i.timestamp < :toMillis
        ORDER BY i.timestamp DESC
        """,
    )
    fun observeBetween(fromMillis: Long, toMillis: Long): Flow<List<IntakeWithType>>

    @Insert
    suspend fun insert(intake: Intake): Long

    @Insert
    suspend fun insertAll(intakes: List<Intake>)

    @Query("DELETE FROM intakes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM intakes ORDER BY timestamp")
    suspend fun getAll(): List<Intake>

    @Query(
        """
        SELECT i.id, i.drinkTypeId, i.amountMl, i.timestamp, t.name AS typeName, t.emoji AS typeEmoji
        FROM intakes i JOIN drink_types t ON t.id = i.drinkTypeId
        ORDER BY i.timestamp
        """,
    )
    suspend fun getAllWithType(): List<IntakeWithType>

    @Query("DELETE FROM intakes")
    suspend fun deleteAll()
}

@Dao
interface DrinkTypeDao {
    @Query("SELECT * FROM drink_types WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<DrinkType>>

    @Query("SELECT * FROM drink_types ORDER BY sortOrder, id")
    suspend fun getAll(): List<DrinkType>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM drink_types")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insert(type: DrinkType): Long

    @Insert
    suspend fun insertAll(types: List<DrinkType>)

    @Update
    suspend fun update(type: DrinkType)

    @Query("UPDATE drink_types SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("DELETE FROM drink_types")
    suspend fun deleteAll()
}
