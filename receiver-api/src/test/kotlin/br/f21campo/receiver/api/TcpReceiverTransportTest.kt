package br.f21campo.receiver.api

import java.net.ServerSocket
import kotlin.concurrent.thread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TcpReceiverTransportTest {
    @Test
    fun connectsToReachableTcpEndpointWithoutSendingProtocolBytes() {
        ServerSocket(0).use { server ->
            val accepted = thread(start = true) { server.accept().use { } }
            val transport = TcpReceiverTransport(connectTimeoutMillis = 1_000)

            val status = transport.connect("127.0.0.1", server.localPort)

            assertEquals(ReceiverConnectionState.CONNECTED, status.state)
            assertTrue(status.message!!.contains("nenhum comando"))
            assertEquals(ReceiverConnectionState.DISCONNECTED, transport.close().state)
            accepted.join(1_000)
        }
    }

    @Test
    fun rejectsInvalidPortBeforeOpeningSocket() {
        val status = TcpReceiverTransport().connect("127.0.0.1", 0)

        assertEquals(ReceiverConnectionState.FAILED, status.state)
        assertTrue(status.message!!.contains("porta TCP válidos"))
    }
}
