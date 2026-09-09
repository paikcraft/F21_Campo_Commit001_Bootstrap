package br.f21campo.domain

object OccupationHeights {
    fun attach(occupation: Occupation, heights: HeightSet): DomainResult<OccupationWithHeights> {
        if (occupation.state == OccupationState.ABORTED) {
            return DomainResult.Failure(DomainError.InvalidTransition(occupation.state.name, "HEIGHTS_ATTACHED"))
        }
        return DomainResult.Success(OccupationWithHeights(occupation, heights))
    }

    fun attachMeasurements(occupation: Occupation, heights: HeightMeasurementSet): DomainResult<OccupationWithHeightMeasurements> {
        if (occupation.state == OccupationState.ABORTED) {
            return DomainResult.Failure(DomainError.InvalidTransition(occupation.state.name, "HEIGHTS_ATTACHED"))
        }
        return DomainResult.Success(OccupationWithHeightMeasurements(occupation, heights))
    }
}

data class OccupationWithHeights(
    val occupation: Occupation,
    val heights: HeightSet,
)

data class OccupationWithHeightMeasurements(
    val occupation: Occupation,
    val heights: HeightMeasurementSet,
)
