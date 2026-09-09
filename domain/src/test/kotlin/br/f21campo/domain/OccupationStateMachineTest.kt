package br.f21campo.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OccupationStateMachineTest {
    private val t = Instant.parse("2026-09-07T12:00:00Z")
    private fun draft(hasBeforeHeight: Boolean = true) = Occupation(EntityId.new(), EntityId.new(), EntityId.new(), referencePointId = EntityId.new(), hasBeforeHeight = hasBeforeHeight, beforeHeightMeters = if (hasBeforeHeight) 1.5 else null)

    @Test fun mainFlowPreservesConfirmedTimestamps() {
        val ready = (OccupationStateMachine.ready(draft()) as DomainResult.Success).value
        val active = (OccupationStateMachine.start(ready, t) as DomainResult.Success).value
        val stopped = (OccupationStateMachine.stop(active, t.plusSeconds(10)) as DomainResult.Success).value
        val collected = (OccupationStateMachine.collect(stopped, hasRawEvidence = true) as DomainResult.Success).value
        val validated = (OccupationStateMachine.validate(collected) as DomainResult.Success).value
        assertEquals(OccupationState.VALIDATED, validated.state)
        assertEquals(t, validated.confirmedStart)
        assertEquals(t.plusSeconds(10), validated.confirmedStop)
    }

    @Test fun collectionRequiresRawEvidence() {
        val stopped = (OccupationStateMachine.stop(
            (OccupationStateMachine.start((OccupationStateMachine.ready(draft()) as DomainResult.Success).value, t) as DomainResult.Success).value,
            t.plusSeconds(1),
        ) as DomainResult.Success).value
        val result = OccupationStateMachine.collect(stopped, hasRawEvidence = false)
        assertTrue(result is DomainResult.Failure)
    }

    @Test fun collectionWithEvidenceRequiresAfterHeight() {
        val stopped = (OccupationStateMachine.stop(
            (OccupationStateMachine.start((OccupationStateMachine.ready(draft()) as DomainResult.Success).value, t) as DomainResult.Success).value,
            t.plusSeconds(1),
        ) as DomainResult.Success).value
        val result = OccupationStateMachine.collectWithEvidence(stopped, hasRawEvidence = true, hasAfterHeight = false)
        assertTrue(result is DomainResult.Failure)
    }

    @Test fun invalidTransitionAndAbortAreExplicit() {
        assertTrue(OccupationStateMachine.stop(draft(), t) is DomainResult.Failure)
        val aborted = OccupationStateMachine.abort(draft()) as DomainResult.Success
        assertEquals(OccupationState.ABORTED, aborted.value.state)
    }

    @Test fun readyRequiresAtLeastOneBeforeHeight() {
        assertTrue(OccupationStateMachine.ready(draft(hasBeforeHeight = false)) is DomainResult.Failure)
    }

    @Test fun readyRequiresReferencePoint() {
        val withoutReference = draft().copy(referencePointId = null)
        assertTrue(OccupationStateMachine.ready(withoutReference) is DomainResult.Failure)
    }
}
