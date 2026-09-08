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
