package br.f21campo.domain

import java.time.Instant

data class Project(
    val id: EntityId,
    val name: String,
    val createdAt: Instant,
    val archivedAt: Instant? = null,
)

data class Station(
    val id: EntityId,
    val name: String,
    val locality: String? = null,
    val municipality: String? = null,
    val createdAt: Instant,
    val archivedAt: Instant? = null,
)

data class Receiver(
    val id: EntityId,
    val manufacturer: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val source: ProvenanceSource = ProvenanceSource.OPERATOR,
)

data class Antenna(
    val id: EntityId,
    val manufacturer: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val source: ProvenanceSource = ProvenanceSource.OPERATOR,
)

enum class OccupationState { DRAFT, READY, ACTIVE, STOPPED, COLLECTED, VALIDATED, ABORTED }

data class EquipmentSnapshot(
    val receiver: Receiver,
    val antenna: Antenna,
)

data class Occupation(
    val id: EntityId,
    val projectId: EntityId,
    val stationId: EntityId,
    val plannedStart: Instant? = null,
    val confirmedStart: Instant? = null,
    val confirmedStop: Instant? = null,
    val state: OccupationState = OccupationState.DRAFT,
    val equipment: EquipmentSnapshot? = null,
    val hasBeforeHeight: Boolean = false,
)

enum class HeightPhase { BEFORE, AFTER }
enum class HeightType { VERTICAL, SLANT, OTHER }

data class HeightObservation(
    val phase: HeightPhase,
    val first: Double,
    val second: Double,
    val third: Double,
    val type: HeightType,
) {
    init {
        require(listOf(first, second, third).all { it.isFinite() }) { "height observations must be finite" }
    }

    val mean: Double get() = (first + second + third) / 3.0
    val range: Double get() = maxOf(first, second, third) - minOf(first, second, third)
}

data class HeightSet(
    val before: HeightObservation,
    val after: HeightObservation,
) {
    val deltaMean: Double get() = after.mean - before.mean
}

enum class OccupationEventCategory { NOTE, OBSTRUCTION, WEATHER, POWER, EQUIPMENT, CONNECTION, OTHER }
enum class EventSeverity { INFO, WARNING, ERROR }

data class OccupationEvent(
    val id: EntityId,
    val occupationId: EntityId,
    val at: Instant,
    val category: OccupationEventCategory,
    val severity: EventSeverity,
    val description: String,
    val source: ProvenanceSource = ProvenanceSource.OPERATOR,
)

enum class StoredFileRole { RAW_OBSERVATION, DERIVED, OTHER }

data class StoredFile(
    val id: EntityId,
    val path: String,
    val sizeBytes: Long,
    val sha256: String,
    val immutableOriginal: Boolean = true,
    val validation: ValidationState = ValidationState.UNKNOWN,
)

data class FileAssociation(
    val id: EntityId,
    val storedFileId: EntityId,
    val ownerId: EntityId,
    val role: StoredFileRole,
)

data class RinexMetadata(
    val storedFileId: EntityId,
    val markerName: String? = null,
    val firstEpoch: Instant? = null,
    val lastEpoch: Instant? = null,
    val validation: ValidationState = ValidationState.UNKNOWN,
)

data class ProcessingResult(
    val id: EntityId,
    val occupationId: EntityId,
    val processor: String,
    val producedAt: Instant,
    val validation: ValidationState = ValidationState.UNKNOWN,
)

data class CoordinateRecord(
    val id: EntityId,
    val occupationId: EntityId,
    val latitude: Double,
    val longitude: Double,
    val height: Double? = null,
    val source: ProvenanceSource,
    val validation: ValidationState = ValidationState.UNKNOWN,
)

data class F21Version(
    val id: EntityId,
    val occupationId: EntityId,
    val version: Int,
    val createdAt: Instant,
    val snapshotHash: String? = null,
)

data class AuditEvent(
    val id: EntityId,
    val at: Instant,
    val action: String,
    val actor: String,
    val entityId: EntityId? = null,
)
