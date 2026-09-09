package br.f21campo.receiver.api

import java.net.InetSocketAddress
import java.net.Socket

/**
 * A protocol-neutral TCP transport for a receiver already reachable over Wi-Fi.
 * It deliberately sends no bytes on connect: identity, framing and commands belong
 * to a model-specific protocol adapter after they have been demonstrated in bench tests.
 */
class TcpReceiverTransport(
    private val connectTimeoutMillis: Int = 5_000,
    private val readTimeoutMillis: Int = 5_000,
) {
    private var socket: Socket? = null

    var status: ReceiverConnectionStatus = ReceiverConnectionStatus(ReceiverConnectionState.DISCONNECTED)
        private set

    @Synchronized
    fun connect(host: String, port: Int): ReceiverConnectionStatus {
        close()
        if (host.isBlank() || port !in 1..65535) {
            return update(ReceiverConnectionState.FAILED, null, "Informe IP/host e porta TCP válidos")
        }
        return try {
            update(ReceiverConnectionState.CONNECTING, "$host:$port", "Abrindo transporte TCP")
            val connectedSocket = Socket()
            connectedSocket.connect(InetSocketAddress(host, port), connectTimeoutMillis)
            connectedSocket.soTimeout = readTimeoutMillis
            socket = connectedSocket
            update(ReceiverConnectionState.CONNECTED, "$host:$port", "TCP conectado; nenhum comando foi enviado")
        } catch (error: Exception) {
            close()
            update(ReceiverConnectionState.FAILED, "$host:$port", "Falha TCP: ${error.javaClass.simpleName}")
        }
    }

    @Synchronized
    fun close(): ReceiverConnectionStatus {
        socket?.runCatching { close() }
        socket = null
        return update(ReceiverConnectionState.DISCONNECTED, null, "Transporte TCP fechado")
    }

    private fun update(state: ReceiverConnectionState, endpoint: String?, message: String) =
        ReceiverConnectionStatus(state, endpoint, message).also { status = it }
}
