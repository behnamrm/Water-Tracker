package io.github.behnamrm.watertracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A kind of drink the user can log (water, tea, …). Removed types are archived so history keeps its labels. */
@Entity(tableName = "drink_types")
data class DrinkType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val sortOrder: Int = 0,
    /** Favorites get a one-tap button next to the drink type dropdown. */
    val favorite: Boolean = false,
    val archived: Boolean = false,
)

/** One logged drink. */
@Entity(
    tableName = "intakes",
    foreignKeys = [
        ForeignKey(entity = DrinkType::class, parentColumns = ["id"], childColumns = ["drinkTypeId"]),
    ],
    indices = [Index("drinkTypeId"), Index("timestamp")],
)
data class Intake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drinkTypeId: Long,
    val amountMl: Int,
    /** Epoch milliseconds. */
    val timestamp: Long,
)

/** An intake joined with its drink type, for display. */
data class IntakeWithType(
    val id: Long,
    val drinkTypeId: Long,
    val amountMl: Int,
    val timestamp: Long,
    val typeName: String,
    val typeEmoji: String,
) {
    fun toIntake() = Intake(id = id, drinkTypeId = drinkTypeId, amountMl = amountMl, timestamp = timestamp)
}
