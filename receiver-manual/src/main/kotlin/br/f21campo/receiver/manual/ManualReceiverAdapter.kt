package br.f21campo.receiver.manual

import br.f21campo.domain.Antenna
import br.f21campo.domain.Receiver
import br.f21campo.receiver.api.ReceiverAdapter
import br.f21campo.receiver.api.ReceiverCapabilities

class ManualReceiverAdapter : ReceiverAdapter {
    override val capabilities = ReceiverCapabilities(
        canConnectAutomatically = false,
        canControlOccupation = false,
        canDetectEquipment = false,
    )

    override fun snapshot(): Pair<Receiver, Antenna>? = null
}
