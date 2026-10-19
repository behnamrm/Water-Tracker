package io.github.behnamrm.watertracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private data class DefaultType(val emoji: String, val name: String, val favorite: Boolean)

/** Drink types created on first launch. They can be renamed, removed or (un)favorited in Settings. */
private val DefaultDrinkTypes = listOf(
    DefaultType("💧", "Water", favorite = true),
    DefaultType("🥤", "Soft drinks", favorite = true),
    DefaultType("🧃", "Non-alcoholic", favorite = false),
    DefaultType("🍺", "Alcoholic", favorite = false),
    DefaultType("🍵", "Tea", favorite = false),
    DefaultType("🥛", "Milk", favorite = true),
    DefaultType("🍹", "Other", favorite = false),
)

/**
 * Version 2 adds Milk as a quick button. If the user still has the original favorites
 * (Water, Soft drinks, Tea), Milk takes Tea's place; otherwise their choice is kept.
 */
private val Migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val hasMilk = db.query("SELECT 1 FROM drink_types WHERE name = 'Milk' AND archived = 0").use { it.moveToFirst() }
        if (hasMilk) return
        val favorites = db.query("SELECT name FROM drink_types WHERE favorite = 1 ORDER BY name").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        val originalFavorites = favorites == listOf("Soft drinks", "Tea", "Water")
        if (originalFavorites) db.execSQL("UPDATE drink_types SET favorite = 0 WHERE name = 'Tea'")
        db.execSQL(
            """
            INSERT INTO drink_types (name, emoji, sortOrder, favorite, archived)
            VALUES ('Milk', '🥛', (SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM drink_types), ?, 0)
            """,
            arrayOf<Any>(if (originalFavorites) 1 else 0),
        )
    }
}

@Database(entities = [DrinkType::class, Intake::class], version = 2, exportSchema = false)
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
                .addMigrations(Migration1To2)
                .build()
    }
}
