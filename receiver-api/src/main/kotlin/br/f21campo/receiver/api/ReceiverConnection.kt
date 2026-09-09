package br.f21campo.receiver.api

/** Neutral transport boundary. It does not assume a Spectra protocol or issue commands. */
enum class ReceiverConnectionState { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

enum class ReceiverTransportType { WIFI_TCP, BLUETOOTH, SERIAL, UNKNOWN }

data class ReceiverConnectionProfile(
    val transportType: ReceiverTransportType,
    val hostOrAddress: String? = null,
    val port: Int? = null,
    val bluetoothName: String? = null,
    val bluetoothMac: String? = null,
    val notes: String? = null,
)

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
