package io.github.behnamrm.watertracker.util

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Formats a volume for display: below 1000 ml it is shown in millilitres ("750 ml"),
 * from 1000 ml on in litres with up to two decimals ("1 L", "1.25 L").
 */
fun formatVolume(ml: Int, locale: Locale = Locale.getDefault()): String =
    if (ml >= 1000) "${litres(ml, "0.##", locale)} L" else "$ml ml"

/** Splits a volume into a number and its unit for large displays: ("750", "ml") or ("1.25", "L"). */
fun formatVolumeParts(ml: Int, locale: Locale = Locale.getDefault()): Pair<String, String> =
    if (ml >= 1000) litres(ml, "0.##", locale) to "L" else ml.toString() to "ml"

/** Groups thousands: 2000 -> "2,000". */
fun formatNumber(value: Int, locale: Locale = Locale.getDefault()): String =
    java.text.NumberFormat.getIntegerInstance(locale).format(value)

/** A compact variant for chart labels: "750", "1.3L". */
fun formatVolumeShort(ml: Int, locale: Locale = Locale.getDefault()): String =
    if (ml >= 1000) "${litres(ml, "0.#", locale)}L" else "$ml"

private fun litres(ml: Int, pattern: String, locale: Locale): String =
    DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale))
        .apply { roundingMode = RoundingMode.HALF_UP }
        .format(ml / 1000.0)
