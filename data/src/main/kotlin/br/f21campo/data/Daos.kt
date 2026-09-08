package br.f21campo.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ProjectDao {
    @Upsert suspend fun upsert(project: ProjectEntity)
    @Query("SELECT * FROM projects WHERE id = :id") suspend fun findById(id: String): ProjectEntity?
    @Query("SELECT * FROM projects ORDER BY createdAtEpochMillis DESC") suspend fun findAll(): List<ProjectEntity>
}

@Dao
interface StationDao {
    @Upsert suspend fun upsert(station: StationEntity)
    @Query("SELECT * FROM stations WHERE id = :id") suspend fun findById(id: String): StationEntity?
    @Query("SELECT * FROM stations ORDER BY createdAtEpochMillis DESC") suspend fun findAll(): List<StationEntity>
}

@Dao
interface ReferencePointDao {
    @Upsert suspend fun upsert(referencePoint: ReferencePointEntity)
    @Query("SELECT * FROM reference_points WHERE id = :id") suspend fun findById(id: String): ReferencePointEntity?
    @Query("SELECT * FROM reference_points WHERE stationId = :stationId ORDER BY code") suspend fun findByStation(stationId: String): List<ReferencePointEntity>
}

@Dao
interface OccupationDao {
    @Upsert suspend fun upsert(occupation: OccupationEntity)
    @Query("SELECT * FROM occupations WHERE id = :id") suspend fun findById(id: String): OccupationEntity?
    @Query("SELECT * FROM occupations WHERE state NOT IN ('COLLECTED', 'VALIDATED', 'ABORTED') ORDER BY confirmedStartEpochMillis DESC, plannedStartEpochMillis DESC") suspend fun findIncomplete(): List<OccupationEntity>
    @Query("SELECT * FROM occupations WHERE stationId = :stationId ORDER BY confirmedStartEpochMillis DESC, plannedStartEpochMillis DESC") suspend fun findByStation(stationId: String): List<OccupationEntity>
}

@Dao
interface OccupationArtifactDao {
    @Upsert suspend fun upsert(artifact: OccupationArtifactEntity)
    @Query("SELECT * FROM occupation_artifacts WHERE occupationId = :occupationId ORDER BY importedAtEpochMillis") suspend fun findByOccupation(occupationId: String): List<OccupationArtifactEntity>
}

@Dao
interface OccupationEventDao {
    @Upsert suspend fun upsert(event: OccupationEventEntity)
    @Query("SELECT * FROM occupation_events WHERE occupationId = :occupationId ORDER BY atEpochMillis") suspend fun findByOccupation(occupationId: String): List<OccupationEventEntity>
}

@Dao
interface HeightMeasurementDao {
    @Upsert suspend fun upsert(measurement: HeightMeasurementEntity)
    @Query("SELECT * FROM height_measurements WHERE occupationId = :occupationId ORDER BY phase, id") suspend fun findByOccupation(occupationId: String): List<HeightMeasurementEntity>
}
