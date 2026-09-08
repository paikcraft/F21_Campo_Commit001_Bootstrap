package br.f21campo.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConnectionReadinessTest {
    @Test fun checklistRequiresAllEvidence() {
        assertFalse(ConnectionReadiness(receiverIdentified = true, antennaIdentified = true).ready)
        assertTrue(ConnectionReadiness(true, true, true, true).ready)
    }
}
