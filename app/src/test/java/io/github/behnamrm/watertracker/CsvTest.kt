package io.github.behnamrm.watertracker

import io.github.behnamrm.watertracker.data.IntakeWithType
import io.github.behnamrm.watertracker.util.CsvExport
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class CsvTest {
    private val zone = ZoneId.of("Asia/Tehran")
    private val today = LocalDate.of(2026, 10, 20) // a Tuesday

    private fun intake(at: LocalDateTime, ml: Int, name: String, typeId: Long = 1) = IntakeWithType(
        id = at.hashCode().toLong(),
        drinkTypeId = typeId,
        amountMl = ml,
        timestamp = at.atZone(zone).toInstant().toEpochMilli(),
        typeName = name,
        typeEmoji = "💧",
    )

    private val sample = listOf(
        intake(LocalDateTime.of(2026, 10, 19, 8, 5, 42), 250, "Water"),
        intake(LocalDateTime.of(2026, 10, 19, 8, 40, 0), 330, "Soft drinks", typeId = 2),
        intake(LocalDateTime.of(2026, 10, 19, 21, 30, 9), 125, "Water"),
        intake(LocalDateTime.of(2026, 10, 20, 7, 0, 0), 200, "Water"),
    )

    private fun files(intakes: List<IntakeWithType> = sample) =
        CsvExport.files(intakes, today, DayOfWeek.MONDAY, zone).toMap().mapValues { (_, csv) ->
            assert(csv.startsWith("\uFEFF"))
            csv.removePrefix("\uFEFF").removeSuffix("\r\n").split("\r\n")
        }

    @Test
    fun exportsFourFiles() {
        assertEquals(listOf("drinks.csv", "hourly.csv", "daily.csv", "weekly.csv"), CsvExport.files(sample, today, DayOfWeek.MONDAY, zone).map { it.first })
    }

    @Test
    fun drinksHaveExactDateTimeTypeAndAmount() {
        val rows = files().getValue("drinks.csv")
        assertEquals("datetime,date,time,weekday,drink_type,amount_ml", rows[0])
        assertEquals("2026-10-19T08:05:42+03:30,2026-10-19,08:05:42,Monday,Water,250", rows[1])
        assertEquals("2026-10-19T08:40:00+03:30,2026-10-19,08:40:00,Monday,Soft drinks,330", rows[2])
        assertEquals(5, rows.size)
    }

    @Test
    fun hourlyCoversEveryHourWithTypeColumns() {
        val rows = files().getValue("hourly.csv")
        // Water drunk most, so it is the first type column.
        assertEquals("hour_start,date,hour,total_ml,drinks,Water_ml,Soft drinks_ml", rows[0])
        assertEquals(1 + 2 * 24, rows.size)
        assertEquals("2026-10-19T00:00,2026-10-19,0,0,0,0,0", rows[1])
        assertEquals("2026-10-19T08:00,2026-10-19,8,580,2,250,330", rows[1 + 8])
        assertEquals("2026-10-19T21:00,2026-10-19,21,125,1,125,0", rows[1 + 21])
        assertEquals("2026-10-20T07:00,2026-10-20,7,200,1,200,0", rows[1 + 24 + 7])
    }

    @Test
    fun dailyHasOneRowPerDayUpToToday() {
        val rows = files().getValue("daily.csv")
        assertEquals(
            listOf(
                "date,weekday,total_ml,drinks,Water_ml,Soft drinks_ml",
                "2026-10-19,Monday,705,3,375,330",
                "2026-10-20,Tuesday,200,1,200,0",
            ),
            rows,
        )
    }

    @Test
    fun weeklyAveragesOverElapsedDays() {
        val rows = files().getValue("weekly.csv")
        // Week of Monday 19th: 905 ml over the 2 days up to today.
        assertEquals(
            listOf(
                "week_start,week_end,total_ml,drinks,daily_average_ml,Water_ml,Soft drinks_ml",
                "2026-10-19,2026-10-25,905,4,452,575,330",
            ),
            rows,
        )
    }

    @Test
    fun quotesNamesWithCommasOrQuotes() {
        val rows = files(
            listOf(
                intake(LocalDateTime.of(2026, 10, 20, 3, 4), 330, "Soda, diet"),
                intake(LocalDateTime.of(2026, 10, 20, 3, 5), 100, "My \"special\" tea", typeId = 2),
            ),
        ).getValue("drinks.csv")
        assertEquals("2026-10-20T03:04:00+03:30,2026-10-20,03:04:00,Tuesday,\"Soda, diet\",330", rows[1])
        assertEquals("2026-10-20T03:05:00+03:30,2026-10-20,03:05:00,Tuesday,\"My \"\"special\"\" tea\",100", rows[2])
    }

    @Test
    fun noDrinksGivesHeadersOnly() {
        files(emptyList()).values.forEach { assertEquals(1, it.size) }
    }
}
