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
