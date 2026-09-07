package br.f21campo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class ManualEquipmentTest {
    @Test fun attachingEquipmentCreatesOccupationSnapshot() {
        val occupation = Occupation(EntityId.new(), EntityId.new(), EntityId.new())
        val receiver = Receiver(EntityId.new(), model = "SP80")
        val antenna = Antenna(EntityId.new(), model = "ATX")
        val result = ManualEquipment.attachSnapshot(occupation, receiver, antenna) as DomainResult.Success
        assertEquals(receiver, result.value.equipment!!.receiver)
        assertNotSame(occupation, result.value)
    }
}
