package br.f21campo.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntitiesTest {
    private val now = Instant.parse("2026-09-07T12:00:00Z")

    @Test fun stationIdentitySurvivesEdit() {
        val id = EntityId.new()
        val original = Station(id, "RN-01", locality = "Manaus", createdAt = now)
        val edited = original.copy(name = "RN-01 atualizado")
        assertEquals(id, edited.id)
        assertNull(edited.municipality)
    }

    @Test fun equipmentIsSnapshotAndHeightsSupportVariableReadings() {
        val receiver = Receiver(EntityId.new(), model = "manual")
        val antenna = Antenna(EntityId.new(), model = "manual")
        val occupation = Occupation(EntityId.new(), EntityId.new(), EntityId.new(), equipment = EquipmentSnapshot(receiver, antenna))
        assertEquals(receiver, occupation.equipment!!.receiver)
        val heights = HeightMeasurementSet(
            listOf(
                HeightMeasurement(HeightPhase.BEFORE, 1.0, HeightType.VERTICAL),
                HeightMeasurement(HeightPhase.AFTER, 1.2, HeightType.VERTICAL),
                HeightMeasurement(HeightPhase.AFTER, 1.4, HeightType.VERTICAL),
            )
        )
        assertEquals(1.0, heights.mean(HeightPhase.BEFORE)!!, 0.000001)
        assertEquals(0.2, heights.amplitude(HeightPhase.AFTER)!!, 0.000001)
        assertEquals(0.3, heights.deltaMean!!, 0.000001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonFiniteHeightIsRejected() {
        HeightObservation(HeightPhase.BEFORE, Double.NaN, 1.0, 1.0, HeightType.OTHER)
    }
}
