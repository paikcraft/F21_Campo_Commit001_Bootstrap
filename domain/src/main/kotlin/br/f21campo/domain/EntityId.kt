package br.f21campo.domain

import java.util.UUID

@JvmInline
value class EntityId(val value: String) {
    init { require(value.isNotBlank()) { "EntityId must not be blank" } }

    companion object {
        fun new(): EntityId = EntityId(UUID.randomUUID().toString())
    }
}
