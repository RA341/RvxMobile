package dev.radn.rvxmobile.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinnedUpdateStatusTest {
    // 2026-01-01T00:00:00Z
    private val installedAt = 1767225600000L

    @Test
    fun newerReleaseHasUpdate() {
        assertTrue(hasPinnedUpdate("2026-01-01T00:00:01Z", installedAt))
    }

    @Test
    fun installedReleaseAndOlderReleasesHaveNoUpdate() {
        assertFalse(hasPinnedUpdate("2026-01-01T00:00:00Z", installedAt))
        assertFalse(hasPinnedUpdate("2025-12-31T23:59:59Z", installedAt))
    }

    @Test
    fun neverInstalledVariantHasNoUpdate() {
        assertFalse(hasPinnedUpdate("2026-01-01T00:00:01Z", 0L))
    }

    @Test
    fun missingOrInvalidReleaseDateHasNoUpdate() {
        listOf(null, "", "invalid", "2026-02-30T00:00:00Z", "2026-01-01T00:00:01Zjunk").forEach {
            assertFalse(hasPinnedUpdate(it, installedAt))
        }
    }
}
