package io.github.behnamrm.watertracker.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class StatsMode { Daily, Weekly }

/** A half-open date range [start, endExclusive). */
data class DateRange(val start: LocalDate, val endExclusive: LocalDate) {
    fun startMillis(zone: ZoneId) = start.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endMillis(zone: ZoneId) = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
    val days: Long get() = ChronoUnit.DAYS.between(start, endExclusive)
    operator fun contains(date: LocalDate) = !date.isBefore(start) && date.isBefore(endExclusive)
}

data class Bucket(val range: DateRange, val totalMl: Int)

data class TypeShare(val name: String, val emoji: String, val totalMl: Int, val fraction: Float)

data class PeriodStats(
    val range: DateRange,
    val buckets: List<Bucket>,
    val totalMl: Int,
    /** Average per day over the days of the period that are not in the future. */
    val dailyAverageMl: Int,
    val best: Bucket?,
    /** Days in the period (not in the future) that reached the goal, and how many such days there are. */
    val goalDays: Int,
    val elapsedDays: Int,
    val byType: List<TypeShare>,
)

object StatsCalculator {
    const val DAYS_PER_PAGE = 7
    const val WEEKS_PER_PAGE = 8

    fun weekStart(date: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    /**
     * The date range shown on a page. Page 0 ends with the current day (daily) or the current
     * week (weekly); each higher page goes one full page further back in time.
     */
    fun pageRange(mode: StatsMode, page: Int, today: LocalDate, firstDayOfWeek: DayOfWeek): DateRange =
        when (mode) {
            StatsMode.Daily -> {
                val end = today.plusDays(1).minusDays(DAYS_PER_PAGE.toLong() * page)
                DateRange(end.minusDays(DAYS_PER_PAGE.toLong()), end)
            }
            StatsMode.Weekly -> {
                val end = weekStart(today, firstDayOfWeek).plusWeeks(1).minusWeeks(WEEKS_PER_PAGE.toLong() * page)
                DateRange(end.minusWeeks(WEEKS_PER_PAGE.toLong()), end)
            }
        }

    fun compute(
        mode: StatsMode,
        range: DateRange,
        intakes: List<IntakeWithType>,
        goalMl: Int,
        today: LocalDate,
        zone: ZoneId,
    ): PeriodStats {
        val perDay = HashMap<LocalDate, Int>()
        for (intake in intakes) {
            val date = Instant.ofEpochMilli(intake.timestamp).atZone(zone).toLocalDate()
            if (date in range) perDay.merge(date, intake.amountMl, Int::plus)
        }

        val step = if (mode == StatsMode.Daily) 1L else 7L
        val buckets = generateSequence(range.start) { it.plusDays(step) }
            .takeWhile { it.isBefore(range.endExclusive) }
            .map { start ->
                val bucketRange = DateRange(start, start.plusDays(step))
                Bucket(bucketRange, perDay.filterKeys { it in bucketRange }.values.sum())
            }
            .toList()

        val total = perDay.values.sum()
        val elapsedEnd = minOf(range.endExclusive, today.plusDays(1))
        val elapsedDays = ChronoUnit.DAYS.between(range.start, elapsedEnd).coerceAtLeast(0).toInt()
        val goalDays = perDay.count { (date, ml) -> date.isBefore(elapsedEnd) && goalMl > 0 && ml >= goalMl }

        val byType = intakes
            .filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() in range }
            .groupBy { it.drinkTypeId }
            .map { (_, list) ->
                val sum = list.sumOf { it.amountMl }
                TypeShare(
                    name = list.first().typeName,
                    emoji = list.first().typeEmoji,
                    totalMl = sum,
                    fraction = if (total > 0) sum.toFloat() / total else 0f,
                )
            }
            .sortedByDescending { it.totalMl }

        return PeriodStats(
            range = range,
            buckets = buckets,
            totalMl = total,
            dailyAverageMl = if (elapsedDays > 0) total / elapsedDays else 0,
            best = buckets.filter { it.totalMl > 0 }.maxByOrNull { it.totalMl },
            goalDays = goalDays,
            elapsedDays = elapsedDays,
            byType = byType,
        )
    }
}
