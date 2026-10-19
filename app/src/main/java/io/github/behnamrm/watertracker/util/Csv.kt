package io.github.behnamrm.watertracker.util

import io.github.behnamrm.watertracker.data.IntakeWithType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Formats drinks as CSV with the columns `date,time,drink_type,amount_ml`, e.g.
 * `2026-10-19,08:30,Water,250`. Starts with a UTF-8 byte order mark so Excel reads
 * non-English drink names correctly.
 */
fun intakesToCsv(intakes: List<IntakeWithType>, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
    append('\uFEFF')
    append("date,time,drink_type,amount_ml\r\n")
    for (intake in intakes) {
        val at = Instant.ofEpochMilli(intake.timestamp).atZone(zone)
        append(at.format(DateFormat)).append(',')
        append(at.format(TimeFormat)).append(',')
        append(csvField(intake.typeName)).append(',')
        append(intake.amountMl).append("\r\n")
    }
}

/** Quotes a field when it contains a comma, quote or line break. */
private fun csvField(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
