package br.f21campo.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ProjectDao {
    @Upsert suspend fun upsert(project: ProjectEntity)
    @Upsert suspend fun upsertAll(projects: List<ProjectEntity>)
    @Query("SELECT * FROM projects WHERE id = :id") suspend fun findById(id: String): ProjectEntity?
    @Query("SELECT * FROM projects ORDER BY createdAtEpochMillis DESC") suspend fun findAll(): List<ProjectEntity>
}

@Dao
interface StationDao {
    @Upsert suspend fun upsert(station: StationEntity)
    @Upsert suspend fun upsertAll(stations: List<StationEntity>)
    @Query("SELECT * FROM stations WHERE id = :id") suspend fun findById(id: String): StationEntity?
    @Query("SELECT * FROM stations ORDER BY createdAtEpochMillis DESC") suspend fun findAll(): List<StationEntity>
}

@Dao
interface ReferencePointDao {
    @Upsert suspend fun upsert(referencePoint: ReferencePointEntity)
    @Upsert suspend fun upsertAll(referencePoints: List<ReferencePointEntity>)
    @Query("SELECT * FROM reference_points WHERE id = :id") suspend fun findById(id: String): ReferencePointEntity?
    @Query("SELECT * FROM reference_points WHERE stationId = :stationId ORDER BY code") suspend fun findByStation(stationId: String): List<ReferencePointEntity>
    @Query("SELECT * FROM reference_points ORDER BY stationId, code") suspend fun findAll(): List<ReferencePointEntity>
}

@Dao
interface OccupationDao {
    @Upsert suspend fun upsert(occupation: OccupationEntity)
    @Query("SELECT * FROM occupations WHERE id = :id") suspend fun findById(id: String): OccupationEntity?
    @Query("SELECT * FROM occupations WHERE state NOT IN ('COLLECTED', 'VALIDATED', 'ABORTED') ORDER BY confirmedStartEpochMillis DESC, plannedStartEpochMillis DESC") suspend fun findIncomplete(): List<OccupationEntity>
    @Query("SELECT * FROM occupations WHERE stationId = :stationId ORDER BY confirmedStartEpochMillis DESC, plannedStartEpochMillis DESC") suspend fun findByStation(stationId: String): List<OccupationEntity>
    @Query("SELECT * FROM occupations ORDER BY plannedStartEpochMillis DESC") suspend fun findAll(): List<OccupationEntity>
}

@Dao
interface OccupationArtifactDao {
    @Upsert suspend fun upsert(artifact: OccupationArtifactEntity)
    @Query("SELECT * FROM occupation_artifacts WHERE occupationId = :occupationId ORDER BY importedAtEpochMillis") suspend fun findByOccupation(occupationId: String): List<OccupationArtifactEntity>
    @Query("SELECT * FROM occupation_artifacts ORDER BY importedAtEpochMillis") suspend fun findAll(): List<OccupationArtifactEntity>
}

@Dao
interface OccupationEventDao {
    @Upsert suspend fun upsert(event: OccupationEventEntity)
    @Query("SELECT * FROM occupation_events WHERE occupationId = :occupationId ORDER BY atEpochMillis") suspend fun findByOccupation(occupationId: String): List<OccupationEventEntity>
    @Query("SELECT * FROM occupation_events ORDER BY atEpochMillis") suspend fun findAll(): List<OccupationEventEntity>
}

@Dao
interface HeightMeasurementDao {
    @Upsert suspend fun upsert(measurement: HeightMeasurementEntity)
    @Query("SELECT * FROM height_measurements WHERE occupationId = :occupationId ORDER BY phase, id") suspend fun findByOccupation(occupationId: String): List<HeightMeasurementEntity>
    @Query("SELECT * FROM height_measurements ORDER BY occupationId, phase, id") suspend fun findAll(): List<HeightMeasurementEntity>
}

@Dao
interface ReceiverConnectionProfileDao {
    @Upsert suspend fun upsert(profile: ReceiverConnectionProfileEntity)
    @Query("SELECT * FROM receiver_connection_profiles ORDER BY savedAtEpochMillis DESC")
    suspend fun findAll(): List<ReceiverConnectionProfileEntity>

    @Query("SELECT * FROM receiver_connection_profiles WHERE isFavorite = 1 ORDER BY savedAtEpochMillis DESC")
    suspend fun findFavorites(): List<ReceiverConnectionProfileEntity>
}
