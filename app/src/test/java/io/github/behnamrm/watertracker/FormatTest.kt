package io.github.behnamrm.watertracker

import io.github.behnamrm.watertracker.util.formatVolume
import io.github.behnamrm.watertracker.util.formatVolumeShort
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormatTest {
    @Test
    fun belowOneLitreShowsMillilitres() {
        assertEquals("0 ml", formatVolume(0, Locale.US))
        assertEquals("250 ml", formatVolume(250, Locale.US))
        assertEquals("999 ml", formatVolume(999, Locale.US))
    }

    @Test
    fun fromOneLitreShowsLitresWithDecimals() {
        assertEquals("1 L", formatVolume(1000, Locale.US))
        assertEquals("1.25 L", formatVolume(1250, Locale.US))
        assertEquals("1.5 L", formatVolume(1500, Locale.US))
        assertEquals("2.08 L", formatVolume(2083, Locale.US))
    }

    @Test
    fun usesLocaleDecimalSeparator() {
        assertEquals("1,25 L", formatVolume(1250, Locale.GERMANY))
    }

    @Test
    fun shortFormatForCharts() {
        assertEquals("750", formatVolumeShort(750, Locale.US))
        assertEquals("1.3L", formatVolumeShort(1250, Locale.US))
        assertEquals("2L", formatVolumeShort(2000, Locale.US))
    }
}
