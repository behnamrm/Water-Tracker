package io.github.behnamrm.watertracker.util

import io.github.behnamrm.watertracker.data.IntakeWithType
import io.github.behnamrm.watertracker.data.StatsCalculator
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * The text export for charts: four CSV files.
 *
 * - `drinks.csv` – every drink: exact date and time, drink type and amount
 * - `hourly.csv`, `daily.csv`, `weekly.csv` – totals per hour, day and week, with one extra
 *   `<drink type>_ml` column per drink type (handy for stacked charts)
 *
 * Totals cover every hour/day/week from the first drink up to [today], including empty ones, so
 * the files can be plotted as continuous time series. Each file starts with a UTF-8 byte order
 * mark so Excel reads non-English drink names correctly.
 */
object CsvExport {
    private val Date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val Time = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val HourStart = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00")
    private val DateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun files(
        intakes: List<IntakeWithType>,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Pair<String, String>> {
        val rows = intakes.sortedBy { it.timestamp }.map { Row(Instant.ofEpochMilli(it.timestamp).atZone(zone), it) }
        val types = rows.groupBy { it.intake.typeName }
            .mapValues { (_, list) -> list.sumOf { it.intake.amountMl } }
            .entries.sortedByDescending { it.value }.map { it.key }
        return listOf(
            "drinks.csv" to drinks(rows),
            "hourly.csv" to hourly(rows, types, today),
            "daily.csv" to daily(rows, types, today),
            "weekly.csv" to weekly(rows, types, today, firstDayOfWeek),
        )
    }

    private class Row(val at: ZonedDateTime, val intake: IntakeWithType) {
        val date: LocalDate get() = at.toLocalDate()
    }

    private fun drinks(rows: List<Row>) = csv(listOf("datetime", "date", "time", "weekday", "drink_type", "amount_ml")) {
        rows.forEach { row ->
            line(
                row.at.truncatedTo(ChronoUnit.SECONDS).format(DateTime),
                row.at.format(Date),
                row.at.format(Time),
                weekday(row.date),
                row.intake.typeName,
                row.intake.amountMl,
            )
        }
    }

    private fun hourly(rows: List<Row>, types: List<String>, today: LocalDate) =
        csv(listOf("hour_start", "date", "hour", "total_ml", "drinks") + typeColumns(types)) {
            val byHour = rows.groupBy { it.date to it.at.hour }
            days(rows, today).forEach { date ->
                for (hour in 0 until 24) {
                    val group = byHour[date to hour].orEmpty()
                    line(date.atTime(hour, 0).format(HourStart), date.format(Date), hour, *totals(group, types))
                }
            }
        }

    private fun daily(rows: List<Row>, types: List<String>, today: LocalDate) =
        csv(listOf("date", "weekday", "total_ml", "drinks") + typeColumns(types)) {
            val byDay = rows.groupBy { it.date }
            days(rows, today).forEach { date ->
                line(date.format(Date), weekday(date), *totals(byDay[date].orEmpty(), types))
            }
        }

    private fun weekly(rows: List<Row>, types: List<String>, today: LocalDate, firstDayOfWeek: DayOfWeek) =
        csv(listOf("week_start", "week_end", "total_ml", "drinks", "daily_average_ml") + typeColumns(types)) {
            if (rows.isEmpty()) return@csv
            val byWeek = rows.groupBy { StatsCalculator.weekStart(it.date, firstDayOfWeek) }
            var start = StatsCalculator.weekStart(rows.first().date, firstDayOfWeek)
            val last = StatsCalculator.weekStart(maxOf(today, rows.last().date), firstDayOfWeek)
            while (!start.isAfter(last)) {
                val group = byWeek[start].orEmpty()
                val total = group.sumOf { it.intake.amountMl }
                // Average over the days of the week that are not in the future.
                val days = ChronoUnit.DAYS.between(start, minOf(start.plusDays(7), today.plusDays(1))).coerceIn(1, 7)
                line(
                    start.format(Date),
                    start.plusDays(6).format(Date),
                    total,
                    group.size,
                    total / days,
                    *types.map { type -> group.filter { it.intake.typeName == type }.sumOf { it.intake.amountMl } }.toTypedArray(),
                )
                start = start.plusWeeks(1)
            }
        }

    /** Every date from the first drink up to [today] (or the last drink, if later). */
    private fun days(rows: List<Row>, today: LocalDate): Sequence<LocalDate> {
        if (rows.isEmpty()) return emptySequence()
        val last = maxOf(today, rows.last().date)
        return generateSequence(rows.first().date) { it.plusDays(1) }.takeWhile { !it.isAfter(last) }
    }

    /** total_ml, drinks, then one amount per drink type. */
    private fun totals(group: List<Row>, types: List<String>): Array<Any> =
        (listOf<Any>(group.sumOf { it.intake.amountMl }, group.size) +
            types.map { type -> group.filter { it.intake.typeName == type }.sumOf { it.intake.amountMl } }).toTypedArray()

    private fun typeColumns(types: List<String>) = types.map { "${it}_ml" }

    private fun weekday(date: LocalDate) = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

    private class CsvBuilder {
        val text = StringBuilder()
        fun line(vararg values: Any) {
            values.joinTo(text, ",") { field(it.toString()) }
            text.append("\r\n")
        }
    }

    private fun csv(header: List<String>, body: CsvBuilder.() -> Unit): String {
        val builder = CsvBuilder()
        builder.text.append('\uFEFF')
        builder.line(*header.toTypedArray())
        builder.body()
        return builder.text.toString()
    }

    /** Quotes a field when it contains a comma, quote or line break. */
    private fun field(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
