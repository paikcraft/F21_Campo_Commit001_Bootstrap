package br.f21campo.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EventsAndAuditTest {
    @Test fun occupationEventsAreSortedAsTimeline() {
        val occupationId = EntityId.new()
        val timeline = OccupationTimeline()
        timeline.append(OccupationEvent(EntityId.new(), occupationId, Instant.parse("2026-09-07T12:02:00Z"), OccupationEventCategory.WEATHER, EventSeverity.WARNING, "chuva"))
        timeline.append(OccupationEvent(EntityId.new(), occupationId, Instant.parse("2026-09-07T12:01:00Z"), OccupationEventCategory.NOTE, EventSeverity.INFO, "início"))
        assertEquals("início", timeline.forOccupation(occupationId).first().description)
    }

    @Test fun auditIsAppendOnlyAndSeparatesActor() {
        val audit = InMemoryAuditService()
        val at = Instant.parse("2026-09-07T12:00:00Z")
        audit.record("OCCUPATION_STARTED", "operator", at)
        audit.record("OCCUPATION_STOPPED", "operator", at.plusSeconds(10))
        assertEquals(2, audit.events().size)
        assertNotEquals(audit.events()[0].id, audit.events()[1].id)
    }
}
