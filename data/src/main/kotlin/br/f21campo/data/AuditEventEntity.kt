package br.f21campo.data

import androidx.room.Entity
import androidx.room.Index

/** Append-only business evidence. It is intentionally not updated or deleted. */
@Entity(
    tableName = "audit_events",
    indices = [Index("entityId"), Index("atEpochMillis")],
)
data class AuditEventEntity(
    @androidx.room.PrimaryKey val id: String,
    val atEpochMillis: Long,
    val action: String,
    val actor: String,
    val entityId: String?,
)
