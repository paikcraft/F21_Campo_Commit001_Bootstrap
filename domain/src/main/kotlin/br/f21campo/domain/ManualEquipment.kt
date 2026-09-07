package br.f21campo.domain

object ManualEquipment {
    fun attachSnapshot(occupation: Occupation, receiver: Receiver, antenna: Antenna): DomainResult<Occupation> {
        if (occupation.state !in setOf(OccupationState.DRAFT, OccupationState.READY)) {
            return DomainResult.Failure(DomainError.InvalidTransition(occupation.state.name, "EQUIPMENT_ATTACHED"))
        }
        return DomainResult.Success(occupation.copy(equipment = EquipmentSnapshot(receiver, antenna)))
    }
}
