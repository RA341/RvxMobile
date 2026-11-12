package dev.radn.rvxmobile.updates

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateFrequencyTest {
    @Test
    fun defaultAndUnknownValuesUseDailyChecks() {
        assertEquals(UpdateFrequency.DAILY, UpdateFrequency.fromHours(24))
        assertEquals(UpdateFrequency.DAILY, UpdateFrequency.fromHours(-1))
    }

    @Test
    fun supportedFrequenciesRoundTrip() {
        UpdateFrequency.entries.forEach { assertEquals(it, UpdateFrequency.fromHours(it.hours)) }
    }
}
