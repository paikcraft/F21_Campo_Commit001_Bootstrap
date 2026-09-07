package br.f21campo.domain

import java.time.Instant

interface AuditService {
    fun append(event: AuditEvent)
    fun events(): List<AuditEvent>
}

class InMemoryAuditService : AuditService {
    private val entries = mutableListOf<AuditEvent>()
    override fun append(event: AuditEvent) { entries += event }
    override fun events(): List<AuditEvent> = entries.toList()
}

class OccupationTimeline {
    private val entries = mutableListOf<OccupationEvent>()

    fun append(event: OccupationEvent) { entries += event }

    fun forOccupation(occupationId: EntityId): List<OccupationEvent> = entries
        .filter { it.occupationId == occupationId }
        .sortedBy { it.at }
}

fun AuditService.record(action: String, actor: String, at: Instant, entityId: EntityId? = null) {
    append(AuditEvent(EntityId.new(), at, action, actor, entityId))
}
