package br.f21campo.data

import br.f21campo.domain.EntityId
import br.f21campo.domain.Project
import br.f21campo.domain.Station
import br.f21campo.domain.Occupation
import br.f21campo.domain.OccupationState
import br.f21campo.domain.ReferencePoint
import br.f21campo.domain.ReferencePointType
import br.f21campo.domain.Receiver
import br.f21campo.domain.Antenna
import br.f21campo.domain.EquipmentSnapshot
import br.f21campo.domain.OccupationSnapshots
import br.f21campo.domain.StationSnapshot
import br.f21campo.domain.ReferencePointSnapshot
import br.f21campo.domain.ReceiverSnapshot
import br.f21campo.domain.AntennaSnapshot
import br.f21campo.domain.ReceiverCatalogItem
import br.f21campo.domain.AntennaCatalogItem
import br.f21campo.domain.AuditEvent
import br.f21campo.domain.ProvenanceSource
import java.time.Instant

fun Project.toEntity() = ProjectEntity(id.value, name, createdAt.toEpochMilli(), archivedAt?.toEpochMilli())
fun ProjectEntity.toDomain() = Project(EntityId(id), name, Instant.ofEpochMilli(createdAtEpochMillis), archivedAtEpochMillis?.let(Instant::ofEpochMilli))
fun Station.toEntity() = StationEntity(id.value, name, locality, municipality, createdAt.toEpochMilli(), archivedAt?.toEpochMilli())
fun StationEntity.toDomain() = Station(EntityId(id), name, locality, municipality, Instant.ofEpochMilli(createdAtEpochMillis), archivedAtEpochMillis?.let(Instant::ofEpochMilli))
fun ReferencePointEntity.toDomain() = ReferencePoint(EntityId(id), EntityId(stationId), ReferencePointType.valueOf(type), code, description, observation)
fun Occupation.toEntity() = OccupationEntity(id.value, projectId?.value, stationId?.value, referencePointId?.value, plannedDurationSeconds, state.name, plannedStart?.toEpochMilli(), confirmedStart?.toEpochMilli(), confirmedStop?.toEpochMilli(), equipment?.receiver?.model, equipment?.antenna?.model, equipment?.receiver?.manufacturer, equipment?.antenna?.manufacturer, equipment?.receiver?.serialNumber, equipment?.antenna?.serialNumber, equipment?.receiver?.firmware, hasBeforeHeight, beforeHeightMeters)
fun OccupationEntity.toDomain(snapshot: OccupationSnapshotEntity? = null): Occupation {
    val flattenedEquipment = if (receiverModel != null || antennaModel != null || receiverSerial != null || antennaSerial != null || receiverManufacturer != null || antennaManufacturer != null || receiverFirmware != null) {
        EquipmentSnapshot(
            Receiver(EntityId.new(), manufacturer = receiverManufacturer, model = receiverModel, serialNumber = receiverSerial, firmware = receiverFirmware),
            Antenna(EntityId.new(), manufacturer = antennaManufacturer, model = antennaModel, serialNumber = antennaSerial),
        )
    } else null
    val snapshotEquipment = snapshot?.let { row ->
        val hasReceiver = row.receiverManufacturer != null || row.receiverModel != null || row.receiverSerial != null || row.receiverFirmware != null
        val hasAntenna = row.antennaManufacturer != null || row.antennaModel != null || row.antennaSerial != null
        if (hasReceiver || hasAntenna) {
            EquipmentSnapshot(
                Receiver(
                    id = row.receiverId?.let(::EntityId) ?: EntityId.new(),
                    manufacturer = row.receiverManufacturer,
                    model = row.receiverModel,
                    serialNumber = row.receiverSerial,
                    firmware = row.receiverFirmware,
                    source = row.receiverSource?.let { runCatching { ProvenanceSource.valueOf(it) }.getOrNull() } ?: ProvenanceSource.OPERATOR,
                ),
                Antenna(
                    id = row.antennaId?.let(::EntityId) ?: EntityId.new(),
                    manufacturer = row.antennaManufacturer,
                    model = row.antennaModel,
                    serialNumber = row.antennaSerial,
                    source = row.antennaSource?.let { runCatching { ProvenanceSource.valueOf(it) }.getOrNull() } ?: ProvenanceSource.OPERATOR,
                ),
            )
        } else null
    }
    return Occupation(
        id = EntityId(id),
        projectId = projectId?.let(::EntityId),
        stationId = stationId?.let(::EntityId),
        referencePointId = referencePointId?.let(::EntityId),
        plannedDurationSeconds = plannedDurationSeconds,
        plannedStart = plannedStartEpochMillis?.let(Instant::ofEpochMilli),
        confirmedStart = confirmedStartEpochMillis?.let(Instant::ofEpochMilli),
        confirmedStop = confirmedStopEpochMillis?.let(Instant::ofEpochMilli),
        state = OccupationState.valueOf(state),
        equipment = snapshotEquipment ?: flattenedEquipment,
        hasBeforeHeight = hasBeforeHeight,
        beforeHeightMeters = beforeHeightMeters,
        snapshots = snapshot?.toDomain(),
    )
}

fun OccupationSnapshotEntity.toDomain() = OccupationSnapshots(
    station = if (stationName != null || stationId != null || stationLocality != null || stationMunicipality != null) StationSnapshot(stationId?.let(::EntityId), stationName.orEmpty(), stationLocality, stationMunicipality) else null,
    referencePoint = if (referencePointId != null || referenceType != null || referenceCode != null || referenceDescription != null || referenceObservation != null) ReferencePointSnapshot(referencePointId?.let(::EntityId), referenceType?.let { runCatching { ReferencePointType.valueOf(it) }.getOrNull() }, referenceCode, referenceDescription, referenceObservation) else null,
    receiver = if (receiverId != null || receiverManufacturer != null || receiverModel != null || receiverSerial != null || receiverFirmware != null) ReceiverSnapshot(receiverId?.let(::EntityId), receiverManufacturer, receiverModel, receiverSerial, receiverFirmware, receiverSource?.let { runCatching { ProvenanceSource.valueOf(it) }.getOrNull() } ?: ProvenanceSource.OPERATOR) else null,
    antenna = if (antennaId != null || antennaManufacturer != null || antennaModel != null || antennaSerial != null) AntennaSnapshot(antennaId?.let(::EntityId), antennaManufacturer, antennaModel, antennaSerial, antennaSource?.let { runCatching { ProvenanceSource.valueOf(it) }.getOrNull() } ?: ProvenanceSource.OPERATOR) else null,
)

fun OccupationSnapshots.toEntity(occupationId: EntityId) = OccupationSnapshotEntity(
    occupationId = occupationId.value,
    stationId = station?.stationId?.value,
    stationName = station?.name,
    stationLocality = station?.locality,
    stationMunicipality = station?.municipality,
    referencePointId = referencePoint?.referencePointId?.value,
    referenceType = referencePoint?.type?.name,
    referenceCode = referencePoint?.code,
    referenceDescription = referencePoint?.description,
    referenceObservation = referencePoint?.observation,
    receiverId = receiver?.receiverId?.value,
    receiverManufacturer = receiver?.manufacturer,
    receiverModel = receiver?.model,
    receiverSerial = receiver?.serialNumber,
    receiverFirmware = receiver?.firmware,
    receiverSource = receiver?.source?.name,
    antennaId = antenna?.antennaId?.value,
    antennaManufacturer = antenna?.manufacturer,
    antennaModel = antenna?.model,
    antennaSerial = antenna?.serialNumber,
    antennaSource = antenna?.source?.name,
)

fun ReceiverCatalogItem.toEntity() = ReceiverCatalogEntity(id.value, manufacturer, model, serialNumber, firmware, createdAt.toEpochMilli(), archivedAt?.toEpochMilli())
fun ReceiverCatalogEntity.toDomain() = ReceiverCatalogItem(EntityId(id), manufacturer, model, serialNumber, firmware, Instant.ofEpochMilli(createdAtEpochMillis), archivedAtEpochMillis?.let(Instant::ofEpochMilli))
fun AntennaCatalogItem.toEntity() = AntennaCatalogEntity(id.value, manufacturer, model, serialNumber, createdAt.toEpochMilli(), archivedAt?.toEpochMilli())
fun AntennaCatalogEntity.toDomain() = AntennaCatalogItem(EntityId(id), manufacturer, model, serialNumber, Instant.ofEpochMilli(createdAtEpochMillis), archivedAtEpochMillis?.let(Instant::ofEpochMilli))
fun AuditEvent.toEntity() = AuditEventEntity(id.value, at.toEpochMilli(), action, actor, entityId?.value)
fun AuditEventEntity.toDomain() = AuditEvent(EntityId(id), Instant.ofEpochMilli(atEpochMillis), action, actor, entityId?.let(::EntityId))
