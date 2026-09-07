package br.f21campo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OccupationHeightsTest {
    @Test fun sixOriginalReadingsAndDerivedValuesRemainAvailable() {
        val occupation = Occupation(EntityId.new(), EntityId.new(), EntityId.new())
        val set = HeightSet(
            HeightObservation(HeightPhase.BEFORE, 1.000, 1.010, 1.020, HeightType.VERTICAL),
            HeightObservation(HeightPhase.AFTER, 1.100, 1.110, 1.120, HeightType.SLANT),
        )
        val result = OccupationHeights.attach(occupation, set) as DomainResult.Success
        assertEquals(6, listOf(set.before.first, set.before.second, set.before.third, set.after.first, set.after.second, set.after.third).size)
        assertEquals(0.02, set.before.range, 0.000001)
        assertEquals(0.10, result.value.heights.deltaMean, 0.000001)
        assertEquals(HeightType.SLANT, result.value.heights.after.type)
    }

    @Test fun abortedOccupationCannotReceiveHeights() {
        val occupation = Occupation(EntityId.new(), EntityId.new(), EntityId.new(), state = OccupationState.ABORTED)
        val set = HeightSet(
            HeightObservation(HeightPhase.BEFORE, 1.0, 1.0, 1.0, HeightType.OTHER),
            HeightObservation(HeightPhase.AFTER, 1.0, 1.0, 1.0, HeightType.OTHER),
        )
        assertTrue(OccupationHeights.attach(occupation, set) is DomainResult.Failure)
    }
}
