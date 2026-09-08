package br.f21campo.domain

import java.time.Duration
import java.time.Instant

data class TrackingTime(
    val elapsedSeconds: Long,
    val plannedSeconds: Long?,
) {
    val isIndefinite: Boolean get() = plannedSeconds == null
    val targetReached: Boolean get() = plannedSeconds != null && elapsedSeconds >= plannedSeconds
}

object TrackingTimer {
    fun measure(start: Instant, now: Instant, plannedSeconds: Long?): TrackingTime =
        TrackingTime(Duration.between(start, now).seconds.coerceAtLeast(0), plannedSeconds)
}
