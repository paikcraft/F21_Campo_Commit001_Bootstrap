package br.f21campo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OccupationHeightsTest {
    @Test fun variableHeightReadingsAndDerivedValuesRemainAvailable() {
        val occupation = Occupation(EntityId.new(), EntityId.new(), EntityId.new())
        val set = HeightMeasurementSet(
            listOf(
                HeightMeasurement(HeightPhase.BEFORE, 1.000, HeightType.VERTICAL),
                HeightMeasurement(HeightPhase.BEFORE, 1.020, HeightType.VERTICAL),
                HeightMeasurement(HeightPhase.AFTER, 1.100, HeightType.SLANT),
            )
        )
        val result = OccupationHeights.attachMeasurements(occupation, set) as DomainResult.Success
        assertEquals(2, set.forPhase(HeightPhase.BEFORE).size)
        assertEquals(0.02, set.amplitude(HeightPhase.BEFORE)!!, 0.000001)
        assertEquals(0.09, result.value.heights.deltaMean!!, 0.000001)
        assertEquals(HeightType.SLANT, set.forPhase(HeightPhase.AFTER).single().type)
    }

    @Test fun abortedOccupationCannotReceiveHeights() {
        val occupation = Occupation(EntityId.new(), EntityId.new(), EntityId.new(), state = OccupationState.ABORTED)
        val set = HeightMeasurementSet(
            listOf(HeightMeasurement(HeightPhase.BEFORE, 1.0, HeightType.OTHER))
        )
        assertTrue(OccupationHeights.attachMeasurements(occupation, set) is DomainResult.Failure)
    }
}
