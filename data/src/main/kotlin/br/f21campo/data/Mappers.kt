package br.f21campo.data

import br.f21campo.domain.EntityId
import br.f21campo.domain.Project
import br.f21campo.domain.Station
import java.time.Instant

fun Project.toEntity() = ProjectEntity(id.value, name, createdAt.toEpochMilli(), archivedAt?.toEpochMilli())
fun ProjectEntity.toDomain() = Project(EntityId(id), name, Instant.ofEpochMilli(createdAtEpochMillis), archivedAtEpochMillis?.let(Instant::ofEpochMilli))
fun Station.toEntity() = StationEntity(id.value, name, locality, municipality, createdAt.toEpochMilli(), archivedAt?.toEpochMilli())
fun StationEntity.toDomain() = Station(EntityId(id), name, locality, municipality, Instant.ofEpochMilli(createdAtEpochMillis), archivedAtEpochMillis?.let(Instant::ofEpochMilli))
