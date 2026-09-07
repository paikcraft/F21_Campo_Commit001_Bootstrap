package br.f21campo.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ProjectDao {
    @Upsert suspend fun upsert(project: ProjectEntity)
    @Query("SELECT * FROM projects WHERE id = :id") suspend fun findById(id: String): ProjectEntity?
}

@Dao
interface StationDao {
    @Upsert suspend fun upsert(station: StationEntity)
    @Query("SELECT * FROM stations WHERE id = :id") suspend fun findById(id: String): StationEntity?
}
