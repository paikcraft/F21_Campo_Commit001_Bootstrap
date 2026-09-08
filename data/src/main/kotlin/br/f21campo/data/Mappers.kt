package br.f21campo.data

import br.f21campo.domain.EntityId
import br.f21campo.domain.Project
import br.f21campo.domain.Station
import br.f21campo.domain.Occupation
import br.f21campo.domain.OccupationState
import java.time.Instant

fun Project.toEntity() = ProjectEntity(id.value, name, createdAt.toEpochMilli(), archivedAt?.toEpochMilli())
fun ProjectEntity.toDomain() = Project(EntityId(id), name, Instant.ofEpochMilli(createdAtEpochMillis), archivedAtEpochMillis?.let(Instant::ofEpochMilli))
fun Station.toEntity() = StationEntity(id.value, name, locality, municipality, createdAt.toEpochMilli(), archivedAt?.toEpochMilli())
fun StationEntity.toDomain() = Station(EntityId(id), name, locality, municipality, Instant.ofEpochMilli(createdAtEpochMillis), archivedAtEpochMillis?.let(Instant::ofEpochMilli))
fun Occupation.toEntity() = OccupationEntity(id.value, projectId.value, stationId.value, referencePointId?.value, plannedDurationSeconds, state.name, plannedStart?.toEpochMilli(), confirmedStart?.toEpochMilli(), confirmedStop?.toEpochMilli(), equipment?.receiver?.model, equipment?.antenna?.model, hasBeforeHeight, beforeHeightMeters)
fun OccupationEntity.toDomain() = Occupation(EntityId(id), EntityId(projectId), EntityId(stationId), referencePointId?.let(::EntityId), plannedDurationSeconds, plannedStartEpochMillis?.let(Instant::ofEpochMilli), confirmedStartEpochMillis?.let(Instant::ofEpochMilli), confirmedStopEpochMillis?.let(Instant::ofEpochMilli), OccupationState.valueOf(state), hasBeforeHeight = hasBeforeHeight, beforeHeightMeters = beforeHeightMeters)
