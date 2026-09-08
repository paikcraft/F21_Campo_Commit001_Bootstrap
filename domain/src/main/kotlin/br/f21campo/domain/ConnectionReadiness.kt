package br.f21campo.domain

/**
 * Checklist neutro para preparar uma conexão de campo.
 * Não representa protocolo, porta ou comando de nenhum fabricante.
 */
data class ConnectionReadiness(
    val receiverIdentified: Boolean = false,
    val antennaIdentified: Boolean = false,
    val transportAvailable: Boolean = false,
    val operatorConfirmed: Boolean = false,
) {
    val ready: Boolean
        get() = receiverIdentified && antennaIdentified && transportAvailable && operatorConfirmed
}
