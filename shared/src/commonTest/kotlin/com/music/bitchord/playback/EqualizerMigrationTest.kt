package com.music.bitchord.playback

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EqualizerMigrationTest {
    @Test fun legacyCurveKeepsItsAnchorsAndShelves() {
        val restored = restoreEqualizerBands(listOf(6f, 4f, 2f, 1f, -2f, -4f, -6f))
        assertEquals(10, restored.size)
        assertEquals(6f, restored.first())
        assertEquals(1f, restored[5])
        assertEquals(-6f, restored.last())
        assertTrue(restored.zipWithNext().all { (a, b) -> a >= b })
    }
    @Test fun newCurveRoundTripsWithoutInterpolation() {
        val values = List(10) { it.toFloat() - 5 }
        assertEquals(values, restoreEqualizerBands(values))
    }
    @Test fun corruptValuesCannotPoisonTheFilters() {
        val values = restoreEqualizerBands(listOf(Float.NaN, Float.POSITIVE_INFINITY, -99f))
        assertTrue(values.all { it.isFinite() && it in -12f..12f })
        assertTrue(manualCurve(listOf(Float.NaN)).gainsDb.all { it.isFinite() })
    }
    @Test fun everyPresetProvidesAllTenBands() {
        EqualizerPreset.entries.filter { it != EqualizerPreset.CUSTOM }.forEach {
            assertEquals(10, it.bands.size)
            assertEquals(it, EqualizerPreset.matching(it.bands))
        }
    }
}
