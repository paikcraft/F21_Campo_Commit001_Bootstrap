package br.f21campo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtEpochMillis: Long,
    val archivedAtEpochMillis: Long?,
)

@Entity(tableName = "stations")
data class StationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val locality: String?,
    val municipality: String?,
    val createdAtEpochMillis: Long,
    val archivedAtEpochMillis: Long?,
)

@Entity(tableName = "reference_points")
data class ReferencePointEntity(
    @PrimaryKey val id: String,
    val stationId: String,
    val type: String,
    val code: String,
    val description: String?,
    val observation: String?,
)

@Entity(tableName = "occupations")
data class OccupationEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val stationId: String,
    val referencePointId: String?,
    val state: String,
    val plannedStartEpochMillis: Long?,
    val confirmedStartEpochMillis: Long?,
    val confirmedStopEpochMillis: Long?,
    val receiverModel: String?,
    val antennaModel: String?,
    val hasBeforeHeight: Boolean,
    val beforeHeightMeters: Double?,
)

@Entity(tableName = "occupation_artifacts")
data class OccupationArtifactEntity(
    @PrimaryKey val id: String,
    val occupationId: String,
    val role: String,
    val path: String,
    val sizeBytes: Long,
    val sha256: String,
    val importedAtEpochMillis: Long,
)

@Entity(tableName = "occupation_events")
data class OccupationEventEntity(
    @PrimaryKey val id: String,
    val occupationId: String,
    val atEpochMillis: Long,
    val category: String,
    val severity: String,
    val description: String,
    val source: String,
)
