package br.f21campo.domain

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingTimerTest {
    private val start = Instant.parse("2026-09-08T10:00:00Z")

    @Test fun undefinedTimeNeverReachesTarget() {
        val result = TrackingTimer.measure(start, start.plusSeconds(3600), null)
        assertTrue(result.isIndefinite)
        assertFalse(result.targetReached)
    }

    @Test fun targetIsReachedAtExactSecond() {
        assertTrue(TrackingTimer.measure(start, start.plusSeconds(14400), 14400).targetReached)
    }
}
