package br.f21campo.receiver.api

/** Neutral transport boundary. It does not assume a Spectra protocol or issue commands. */
enum class ReceiverConnectionState { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

data class ReceiverConnectionStatus(
    val state: ReceiverConnectionState,
    val endpoint: String? = null,
    val message: String? = null,
)

interface ReceiverConnection {
    val status: ReceiverConnectionStatus
    fun connect(endpoint: String): ReceiverConnectionStatus
    fun disconnect(): ReceiverConnectionStatus
}
