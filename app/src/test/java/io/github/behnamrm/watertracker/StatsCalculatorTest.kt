package io.github.behnamrm.watertracker

import io.github.behnamrm.watertracker.data.DateRange
import io.github.behnamrm.watertracker.data.IntakeWithType
import io.github.behnamrm.watertracker.data.StatsCalculator
import io.github.behnamrm.watertracker.data.StatsMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class StatsCalculatorTest {
    private val zone = ZoneId.of("Europe/Berlin")
    private val today = LocalDate.of(2026, 10, 21) // a Wednesday

    private fun intake(at: LocalDateTime, ml: Int, typeId: Long = 1, name: String = "Water") =
        IntakeWithType(
            id = at.hashCode().toLong() + ml,
            drinkTypeId = typeId,
            amountMl = ml,
            timestamp = at.atZone(zone).toInstant().toEpochMilli(),
            typeName = name,
            typeEmoji = "💧",
        )

    @Test
    fun dailyPagesCoverSevenDaysEndingToday() {
        val page0 = StatsCalculator.pageRange(StatsMode.Daily, 0, today, DayOfWeek.MONDAY)
        assertEquals(DateRange(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 22)), page0)
        val page1 = StatsCalculator.pageRange(StatsMode.Daily, 1, today, DayOfWeek.MONDAY)
        assertEquals(DateRange(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 15)), page1)
    }

    @Test
    fun weeklyPagesCoverEightWeeksEndingThisWeek() {
        val range = StatsCalculator.pageRange(StatsMode.Weekly, 0, today, DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 10, 26), range.endExclusive) // next Monday
        assertEquals(LocalDate.of(2026, 8, 31), range.start)
        assertEquals(56, range.days)
    }

    @Test
    fun dailyBucketsSumPerDayAndIgnoreOutOfRange() {
        val range = StatsCalculator.pageRange(StatsMode.Daily, 0, today, DayOfWeek.MONDAY)
        val intakes = listOf(
            intake(LocalDateTime.of(2026, 10, 21, 8, 0), 250),
            intake(LocalDateTime.of(2026, 10, 21, 23, 59), 500),
            intake(LocalDateTime.of(2026, 10, 15, 0, 0), 1000),
            intake(LocalDateTime.of(2026, 10, 14, 23, 59), 999), // before the range
        )
        val stats = StatsCalculator.compute(StatsMode.Daily, range, intakes, goalMl = 750, today = today, zone = zone)

        assertEquals(7, stats.buckets.size)
        assertEquals(1000, stats.buckets.first().totalMl)
        assertEquals(750, stats.buckets.last().totalMl)
        assertEquals(1750, stats.totalMl)
        assertEquals(250, stats.dailyAverageMl)
        assertEquals(2, stats.goalDays)
        assertEquals(7, stats.elapsedDays)
        assertEquals(1000, stats.best?.totalMl)
    }

    @Test
    fun weeklyAverageOnlyCountsDaysUpToToday() {
        val range = StatsCalculator.pageRange(StatsMode.Weekly, 0, today, DayOfWeek.MONDAY)
        val intakes = listOf(
            intake(LocalDateTime.of(2026, 10, 19, 9, 0), 1400), // Monday of this week
            intake(LocalDateTime.of(2026, 9, 1, 9, 0), 700),
        )
        val stats = StatsCalculator.compute(StatsMode.Weekly, range, intakes, goalMl = 2000, today = today, zone = zone)

        assertEquals(8, stats.buckets.size)
        assertEquals(700, stats.buckets.first().totalMl)
        assertEquals(1400, stats.buckets.last().totalMl)
        // Aug 31 .. Oct 21 inclusive = 52 days elapsed; the rest of this week is in the future.
        assertEquals(52, stats.elapsedDays)
        assertEquals(2100 / 52, stats.dailyAverageMl)
    }

    @Test
    fun breakdownByTypeIsSortedWithFractions() {
        val range = StatsCalculator.pageRange(StatsMode.Daily, 0, today, DayOfWeek.MONDAY)
        val intakes = listOf(
            intake(LocalDateTime.of(2026, 10, 20, 9, 0), 300, typeId = 1, name = "Water"),
            intake(LocalDateTime.of(2026, 10, 20, 10, 0), 600, typeId = 2, name = "Tea"),
            intake(LocalDateTime.of(2026, 10, 21, 10, 0), 500, typeId = 1, name = "Water"),
        )
        val stats = StatsCalculator.compute(StatsMode.Daily, range, intakes, goalMl = 2000, today = today, zone = zone)

        assertEquals(listOf("Water", "Tea"), stats.byType.map { it.name })
        assertEquals(800, stats.byType[0].totalMl)
        assertEquals(800f / 1400, stats.byType[0].fraction, 0.001f)
        assertEquals(600f / 1400, stats.byType[1].fraction, 0.001f)
    }

    @Test
    fun hourlyPagesAreSingleDays() {
        assertEquals(DateRange(today, today.plusDays(1)), StatsCalculator.pageRange(StatsMode.Hourly, 0, today, DayOfWeek.MONDAY))
        assertEquals(
            DateRange(today.minusDays(2), today.minusDays(1)),
            StatsCalculator.pageRange(StatsMode.Hourly, 2, today, DayOfWeek.MONDAY),
        )
    }

    @Test
    fun hourlyBucketsSumPerHourOfTheDay() {
        val range = StatsCalculator.pageRange(StatsMode.Hourly, 0, today, DayOfWeek.MONDAY)
        val intakes = listOf(
            intake(LocalDateTime.of(2026, 10, 21, 8, 0), 250),
            intake(LocalDateTime.of(2026, 10, 21, 8, 59), 100),
            intake(LocalDateTime.of(2026, 10, 21, 23, 30), 500),
            intake(LocalDateTime.of(2026, 10, 20, 8, 30), 999), // yesterday
        )
        val stats = StatsCalculator.compute(StatsMode.Hourly, range, intakes, goalMl = 800, today = today, zone = zone)

        assertEquals(24, stats.buckets.size)
        assertEquals((0 until 24).toList(), stats.buckets.map { it.hour })
        assertEquals(350, stats.buckets[8].totalMl)
        assertEquals(500, stats.buckets[23].totalMl)
        assertEquals(0, stats.buckets[12].totalMl)
        assertEquals(850, stats.totalMl)
        assertEquals(3, stats.drinkCount)
        assertEquals(23, stats.best?.hour)
        assertEquals(1, stats.goalDays)
    }
}
