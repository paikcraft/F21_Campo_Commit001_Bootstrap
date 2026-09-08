package br.f21campo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [ProjectEntity::class, StationEntity::class, ReferencePointEntity::class, OccupationEntity::class], version = 5, exportSchema = true)
@TypeConverters(F21Converters::class)
abstract class F21Database : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun stationDao(): StationDao
    abstract fun referencePointDao(): ReferencePointDao
    abstract fun occupationDao(): OccupationDao
}
