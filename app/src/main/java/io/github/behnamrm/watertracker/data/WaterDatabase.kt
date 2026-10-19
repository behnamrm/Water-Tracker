package io.github.behnamrm.watertracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

private data class DefaultType(val emoji: String, val name: String, val favorite: Boolean)

/** Drink types created on first launch. They can be renamed, removed or (un)favorited in Settings. */
private val DefaultDrinkTypes = listOf(
    DefaultType("💧", "Water", favorite = true),
    DefaultType("🥤", "Soft drinks", favorite = true),
    DefaultType("🧃", "Non-alcoholic", favorite = false),
    DefaultType("🍺", "Alcoholic", favorite = false),
    DefaultType("🍵", "Tea", favorite = true),
    DefaultType("🍹", "Other", favorite = false),
)

@Database(entities = [DrinkType::class, Intake::class], version = 1, exportSchema = false)
abstract class WaterDatabase : RoomDatabase() {
    abstract fun intakeDao(): IntakeDao
    abstract fun drinkTypeDao(): DrinkTypeDao

    companion object {
        fun build(context: Context): WaterDatabase =
            Room.databaseBuilder(context, WaterDatabase::class.java, "water.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        DefaultDrinkTypes.forEachIndexed { index, type ->
                            db.execSQL(
                                "INSERT INTO drink_types (name, emoji, sortOrder, favorite, archived) VALUES (?, ?, ?, ?, 0)",
                                arrayOf<Any>(type.name, type.emoji, index, if (type.favorite) 1 else 0),
                            )
                        }
                    }
                })
                .build()
    }
}
