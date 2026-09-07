package br.f21campo.receiver.manual

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ManualReceiverAdapterTest {
    @Test fun manualAdapterNeverClaimsAutomaticCapabilities() {
        val adapter = ManualReceiverAdapter()
        assertFalse(adapter.capabilities.canConnectAutomatically)
        assertFalse(adapter.capabilities.canControlOccupation)
        assertFalse(adapter.capabilities.canDetectEquipment)
        assertNull(adapter.snapshot())
    }
}
