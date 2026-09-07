package br.f21campo.receiver.api

import br.f21campo.domain.Antenna
import br.f21campo.domain.Receiver

data class ReceiverCapabilities(
    val canConnectAutomatically: Boolean,
    val canControlOccupation: Boolean,
    val canDetectEquipment: Boolean,
)

interface ReceiverAdapter {
    val capabilities: ReceiverCapabilities
    fun snapshot(): Pair<Receiver, Antenna>?
}
