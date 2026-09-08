package br.f21campo.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ReferencePointTest {
    @Test fun supportsOneOrManyMeasurementsWithoutFixedThreePlusThreeRule() {
        val set = HeightMeasurementSet(listOf(HeightMeasurement(HeightPhase.BEFORE, 1.25, HeightType.VERTICAL)))
        assertEquals(1.25, set.mean(HeightPhase.BEFORE)!!, 0.000001)
        assertEquals(null, set.deltaMean)
    }
}
