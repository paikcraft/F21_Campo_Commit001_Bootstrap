package br.f21campo.domain

sealed interface DomainError {
    data class InvalidValue(val field: String, val reason: String) : DomainError
    data class NotFound(val entity: String, val id: EntityId) : DomainError
    data class InvalidTransition(val from: String, val to: String) : DomainError
}

sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}
