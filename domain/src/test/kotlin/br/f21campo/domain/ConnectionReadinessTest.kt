package br.f21campo.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionReadinessTest {
    @Test fun checklistRequiresAllEvidence() {
        assertFalse(ConnectionReadiness(receiverIdentified = true, antennaIdentified = true).ready)
        assertTrue(ConnectionReadiness(true, true, true, true).ready)
    }
}
