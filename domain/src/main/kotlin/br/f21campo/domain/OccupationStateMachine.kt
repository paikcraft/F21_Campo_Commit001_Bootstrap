package br.f21campo.domain

import java.time.Instant

object OccupationStateMachine {
    fun ready(occupation: Occupation): DomainResult<Occupation> {
        if (!occupation.hasBeforeHeight || occupation.beforeHeightMeters == null) return DomainResult.Failure(DomainError.InvalidValue("beforeHeight", "at least one BEFORE measurement is required before READY"))
        return transition(occupation, OccupationState.READY) {
        occupation.copy(state = OccupationState.READY)
    }
    }

    fun start(occupation: Occupation, at: Instant): DomainResult<Occupation> = transition(occupation, OccupationState.ACTIVE) {
        occupation.copy(state = OccupationState.ACTIVE, confirmedStart = occupation.confirmedStart ?: at)
    }

    fun stop(occupation: Occupation, at: Instant): DomainResult<Occupation> = transition(occupation, OccupationState.STOPPED) {
        occupation.copy(state = OccupationState.STOPPED, confirmedStop = occupation.confirmedStop ?: at)
    }

    fun collect(occupation: Occupation, hasRawEvidence: Boolean): DomainResult<Occupation> {
        if (!hasRawEvidence) return DomainResult.Failure(DomainError.InvalidValue("rawEvidence", "required before COLLECTED"))
        return transition(occupation, OccupationState.COLLECTED) { occupation.copy(state = OccupationState.COLLECTED) }
    }

    fun validate(occupation: Occupation): DomainResult<Occupation> = transition(occupation, OccupationState.VALIDATED) {
        occupation.copy(state = OccupationState.VALIDATED)
    }

    fun abort(occupation: Occupation): DomainResult<Occupation> {
        if (occupation.state !in setOf(OccupationState.DRAFT, OccupationState.READY, OccupationState.ACTIVE)) {
            return DomainResult.Failure(DomainError.InvalidTransition(occupation.state.name, OccupationState.ABORTED.name))
        }
        return DomainResult.Success(occupation.copy(state = OccupationState.ABORTED))
    }

    private fun transition(occupation: Occupation, target: OccupationState, update: () -> Occupation): DomainResult<Occupation> {
        val valid = when (occupation.state to target) {
            OccupationState.DRAFT to OccupationState.READY,
            OccupationState.READY to OccupationState.ACTIVE,
            OccupationState.ACTIVE to OccupationState.STOPPED,
            OccupationState.STOPPED to OccupationState.COLLECTED,
            OccupationState.COLLECTED to OccupationState.VALIDATED -> true
            else -> false
        }
        return if (valid) DomainResult.Success(update())
        else DomainResult.Failure(DomainError.InvalidTransition(occupation.state.name, target.name))
    }
}
