package br.f21campo.data

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "receiver_catalog", indices = [Index("createdAtEpochMillis")])
data class ReceiverCatalogEntity(
    @androidx.room.PrimaryKey val id: String,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val firmware: String?,
    val createdAtEpochMillis: Long,
    val archivedAtEpochMillis: Long?,
)

@Entity(tableName = "antenna_catalog", indices = [Index("createdAtEpochMillis")])
data class AntennaCatalogEntity(
    @androidx.room.PrimaryKey val id: String,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val createdAtEpochMillis: Long,
    val archivedAtEpochMillis: Long?,
)
