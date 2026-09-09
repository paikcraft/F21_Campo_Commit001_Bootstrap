package br.f21campo.receiver.manual

import br.f21campo.domain.Antenna
import br.f21campo.domain.Receiver
import br.f21campo.receiver.api.ReceiverAdapter
import br.f21campo.receiver.api.ReceiverCapabilities
import br.f21campo.receiver.api.ReceiverConnection
import br.f21campo.receiver.api.ReceiverConnectionState
import br.f21campo.receiver.api.ReceiverConnectionStatus

class ManualReceiverAdapter : ReceiverAdapter {
    override val capabilities = ReceiverCapabilities(
        canConnectAutomatically = false,
        canControlOccupation = false,
        canDetectEquipment = false,
    )

    override fun snapshot(): Pair<Receiver, Antenna>? = null
}

class ManualReceiverConnection : ReceiverConnection {
    override var status = ReceiverConnectionStatus(
        state = ReceiverConnectionState.DISCONNECTED,
        message = "Conexão automática não implementada para o modo manual",
    )
        private set

    override fun connect(endpoint: String): ReceiverConnectionStatus {
        status = ReceiverConnectionStatus(
            state = ReceiverConnectionState.FAILED,
            endpoint = endpoint.ifBlank { null },
            message = "Perfil salvo para bancada; protocolo/porta/framing ainda não homologados",
        )
        return status
    }

    override fun disconnect(): ReceiverConnectionStatus {
        status = ReceiverConnectionStatus(
            state = ReceiverConnectionState.DISCONNECTED,
            message = "Desconectado",
        )
        return status
    }
}
