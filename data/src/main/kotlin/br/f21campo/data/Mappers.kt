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
import java.time.Instant

fun Project.toEntity() = ProjectEntity(id.value, name, createdAt.toEpochMilli(), archivedAt?.toEpochMilli())
fun ProjectEntity.toDomain() = Project(EntityId(id), name, Instant.ofEpochMilli(createdAtEpochMillis), archivedAtEpochMillis?.let(Instant::ofEpochMilli))
fun Station.toEntity() = StationEntity(id.value, name, locality, municipality, createdAt.toEpochMilli(), archivedAt?.toEpochMilli())
fun StationEntity.toDomain() = Station(EntityId(id), name, locality, municipality, Instant.ofEpochMilli(createdAtEpochMillis), archivedAtEpochMillis?.let(Instant::ofEpochMilli))
fun ReferencePointEntity.toDomain() = ReferencePoint(EntityId(id), EntityId(stationId), ReferencePointType.valueOf(type), code, description, observation)
fun Occupation.toEntity() = OccupationEntity(id.value, projectId.value, stationId.value, referencePointId?.value, plannedDurationSeconds, state.name, plannedStart?.toEpochMilli(), confirmedStart?.toEpochMilli(), confirmedStop?.toEpochMilli(), equipment?.receiver?.model, equipment?.antenna?.model, equipment?.receiver?.serialNumber, equipment?.antenna?.serialNumber, equipment?.receiver?.manufacturer, equipment?.antenna?.manufacturer, hasBeforeHeight, beforeHeightMeters)
fun OccupationEntity.toDomain() = Occupation(EntityId(id), EntityId(projectId), EntityId(stationId), referencePointId?.let(::EntityId), plannedDurationSeconds, plannedStartEpochMillis?.let(Instant::ofEpochMilli), confirmedStartEpochMillis?.let(Instant::ofEpochMilli), confirmedStopEpochMillis?.let(Instant::ofEpochMilli), OccupationState.valueOf(state), equipment = if (receiverModel != null || antennaModel != null || receiverSerial != null || antennaSerial != null || receiverManufacturer != null || antennaManufacturer != null) EquipmentSnapshot(Receiver(EntityId.new(), manufacturer = receiverManufacturer, model = receiverModel, serialNumber = receiverSerial), Antenna(EntityId.new(), manufacturer = antennaManufacturer, model = antennaModel, serialNumber = antennaSerial)) else null, hasBeforeHeight = hasBeforeHeight, beforeHeightMeters = beforeHeightMeters)
