package br.f21campo.receiver.manual

import br.f21campo.receiver.api.ReceiverConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualReceiverConnectionTest {
    @Test
    fun connectDoesNotPretendAutomaticReceiverCommunication() {
        val connection = ManualReceiverConnection()

        val status = connection.connect("192.168.0.1:0000")

        assertEquals(ReceiverConnectionState.FAILED, status.state)
        assertEquals("192.168.0.1:0000", status.endpoint)
        assertTrue(status.message!!.contains("ainda não homologados"))
    }

    @Test
    fun disconnectReturnsDisconnectedState() {
        val connection = ManualReceiverConnection()

        connection.connect("BT:Spectra")
        val status = connection.disconnect()

        assertEquals(ReceiverConnectionState.DISCONNECTED, status.state)
    }
}
