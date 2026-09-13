package br.f21campo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ProjectEntity::class, StationEntity::class, ReferencePointEntity::class, OccupationEntity::class, OccupationSnapshotEntity::class, OccupationArtifactEntity::class, OccupationEventEntity::class, HeightMeasurementEntity::class, AuditEventEntity::class, ReceiverConnectionProfileEntity::class, ReceiverCatalogEntity::class, AntennaCatalogEntity::class], version = 19, exportSchema = true)
@TypeConverters(F21Converters::class)
abstract class F21Database : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun stationDao(): StationDao
    abstract fun referencePointDao(): ReferencePointDao
    abstract fun occupationDao(): OccupationDao
    abstract fun occupationSnapshotDao(): OccupationSnapshotDao
    abstract fun occupationArtifactDao(): OccupationArtifactDao
    abstract fun occupationEventDao(): OccupationEventDao
    abstract fun heightMeasurementDao(): HeightMeasurementDao
    abstract fun auditEventDao(): AuditEventDao
    abstract fun receiverConnectionProfileDao(): ReceiverConnectionProfileDao
    abstract fun receiverCatalogDao(): ReceiverCatalogDao
    abstract fun antennaCatalogDao(): AntennaCatalogDao
}
