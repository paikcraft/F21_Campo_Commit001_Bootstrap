package br.f21campo.domain

enum class ProvenanceSource {
    PLANNED,
    CONFIRMED,
    DERIVED,
    OPERATOR,
    EQUIPMENT,
    IMPORTED,
    VALIDATED,
    APPROVED,
}

enum class ValidationState { UNKNOWN, PENDING, VALID, INVALID }
