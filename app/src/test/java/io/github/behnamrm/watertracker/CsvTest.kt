package io.github.behnamrm.watertracker

import io.github.behnamrm.watertracker.data.IntakeWithType
import io.github.behnamrm.watertracker.util.intakesToCsv
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CsvTest {
    private val zone = ZoneId.of("Asia/Tehran")

    private fun intake(at: LocalDateTime, ml: Int, name: String) = IntakeWithType(
        id = 1,
        drinkTypeId = 1,
        amountMl = ml,
        timestamp = at.atZone(zone).toInstant().toEpochMilli(),
        typeName = name,
        typeEmoji = "💧",
    )

    @Test
    fun writesHeaderAndOneRowPerDrink() {
        val csv = intakesToCsv(
            listOf(
                intake(LocalDateTime.of(2026, 10, 19, 8, 5), 250, "Water"),
                intake(LocalDateTime.of(2026, 10, 19, 21, 30), 125, "چای"),
            ),
            zone,
        )
        assertEquals(
            "\uFEFFdate,time,drink_type,amount_ml\r\n" +
                "2026-10-19,08:05,Water,250\r\n" +
                "2026-10-19,21:30,چای,125\r\n",
            csv,
        )
    }

    @Test
    fun quotesNamesWithCommasOrQuotes() {
        val csv = intakesToCsv(
            listOf(
                intake(LocalDateTime.of(2026, 1, 2, 3, 4), 330, "Soda, diet"),
                intake(LocalDateTime.of(2026, 1, 2, 3, 5), 100, "My \"special\" tea"),
            ),
            zone,
        )
        val rows = csv.removePrefix("\uFEFF").split("\r\n")
        assertEquals("2026-01-02,03:04,\"Soda, diet\",330", rows[1])
        assertEquals("2026-01-02,03:05,\"My \"\"special\"\" tea\",100", rows[2])
    }

    @Test
    fun emptyExportHasOnlyTheHeader() {
        assertEquals("\uFEFFdate,time,drink_type,amount_ml\r\n", intakesToCsv(emptyList(), zone))
    }
}
