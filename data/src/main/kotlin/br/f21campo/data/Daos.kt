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
    @Query("SELECT * FROM projects WHERE archivedAtEpochMillis IS NULL ORDER BY createdAtEpochMillis DESC") suspend fun findActive(): List<ProjectEntity>
    @Query("SELECT * FROM projects WHERE archivedAtEpochMillis IS NULL AND lower(name) = lower(:name) LIMIT 1") suspend fun findActiveByName(name: String): ProjectEntity?
    @Query("UPDATE projects SET archivedAtEpochMillis = :archivedAtEpochMillis WHERE id = :id") suspend fun archive(id: String, archivedAtEpochMillis: Long)
}

@Dao
interface StationDao {
    @Upsert suspend fun upsert(station: StationEntity)
    @Upsert suspend fun upsertAll(stations: List<StationEntity>)
    @Query("SELECT * FROM stations WHERE id = :id") suspend fun findById(id: String): StationEntity?
    @Query("SELECT * FROM stations ORDER BY createdAtEpochMillis DESC") suspend fun findAll(): List<StationEntity>
    @Query("SELECT * FROM stations WHERE archivedAtEpochMillis IS NULL ORDER BY createdAtEpochMillis DESC") suspend fun findActive(): List<StationEntity>
    @Query("SELECT * FROM stations WHERE archivedAtEpochMillis IS NULL AND name = :name AND ((locality = :locality) OR (locality IS NULL AND :locality IS NULL)) LIMIT 1") suspend fun findActiveByIdentity(name: String, locality: String?): StationEntity?
    @Query("UPDATE stations SET archivedAtEpochMillis = :archivedAtEpochMillis WHERE id = :id") suspend fun archive(id: String, archivedAtEpochMillis: Long)
}

@Dao
interface ReferencePointDao {
    @Upsert suspend fun upsert(referencePoint: ReferencePointEntity)
    @Upsert suspend fun upsertAll(referencePoints: List<ReferencePointEntity>)
    @Query("SELECT * FROM reference_points WHERE id = :id") suspend fun findById(id: String): ReferencePointEntity?
    @Query("SELECT * FROM reference_points WHERE stationId = :stationId ORDER BY code") suspend fun findByStation(stationId: String): List<ReferencePointEntity>
    @Query("SELECT * FROM reference_points ORDER BY stationId, code") suspend fun findAll(): List<ReferencePointEntity>
    @Query("SELECT * FROM reference_points WHERE stationId = :stationId AND type = :type AND code = :code LIMIT 1") suspend fun findByStationTypeAndCode(stationId: String, type: String, code: String): ReferencePointEntity?
}

@Dao
interface OccupationDao {
    @Upsert suspend fun upsert(occupation: OccupationEntity)
    @Upsert suspend fun upsertAll(occupations: List<OccupationEntity>)
    @Query("SELECT * FROM occupations WHERE id = :id") suspend fun findById(id: String): OccupationEntity?
    @Query("SELECT * FROM occupations WHERE state NOT IN ('COLLECTED', 'VALIDATED', 'ABORTED') ORDER BY confirmedStartEpochMillis DESC, plannedStartEpochMillis DESC") suspend fun findIncomplete(): List<OccupationEntity>
    @Query("SELECT * FROM occupations WHERE stationId = :stationId ORDER BY confirmedStartEpochMillis DESC, plannedStartEpochMillis DESC") suspend fun findByStation(stationId: String): List<OccupationEntity>
    @Query("SELECT * FROM occupations ORDER BY plannedStartEpochMillis DESC") suspend fun findAll(): List<OccupationEntity>
}

@Dao
interface OccupationArtifactDao {
    @Upsert suspend fun upsert(artifact: OccupationArtifactEntity)
    @Upsert suspend fun upsertAll(artifacts: List<OccupationArtifactEntity>)
    @Query("SELECT * FROM occupation_artifacts WHERE occupationId = :occupationId ORDER BY importedAtEpochMillis") suspend fun findByOccupation(occupationId: String): List<OccupationArtifactEntity>
    @Query("SELECT * FROM occupation_artifacts ORDER BY importedAtEpochMillis") suspend fun findAll(): List<OccupationArtifactEntity>
}

@Dao
interface OccupationEventDao {
    @Upsert suspend fun upsert(event: OccupationEventEntity)
    @Upsert suspend fun upsertAll(events: List<OccupationEventEntity>)
    @Query("SELECT * FROM occupation_events WHERE occupationId = :occupationId ORDER BY atEpochMillis") suspend fun findByOccupation(occupationId: String): List<OccupationEventEntity>
    @Query("SELECT * FROM occupation_events ORDER BY atEpochMillis") suspend fun findAll(): List<OccupationEventEntity>
}

@Dao
interface HeightMeasurementDao {
    @Upsert suspend fun upsert(measurement: HeightMeasurementEntity)
    @Upsert suspend fun upsertAll(measurements: List<HeightMeasurementEntity>)
    @Query("DELETE FROM height_measurements WHERE occupationId = :occupationId AND phase = :phase") suspend fun deleteByOccupationAndPhase(occupationId: String, phase: String)
    @Query("SELECT * FROM height_measurements WHERE occupationId = :occupationId ORDER BY phase, id") suspend fun findByOccupation(occupationId: String): List<HeightMeasurementEntity>
    @Query("SELECT * FROM height_measurements ORDER BY occupationId, phase, id") suspend fun findAll(): List<HeightMeasurementEntity>
}

@Dao
interface ReceiverConnectionProfileDao {
    @Upsert suspend fun upsert(profile: ReceiverConnectionProfileEntity)
    @Upsert suspend fun upsertAll(profiles: List<ReceiverConnectionProfileEntity>)
    @Query("SELECT * FROM receiver_connection_profiles ORDER BY savedAtEpochMillis DESC")
    suspend fun findAll(): List<ReceiverConnectionProfileEntity>

    @Query("SELECT * FROM receiver_connection_profiles WHERE isFavorite = 1 ORDER BY savedAtEpochMillis DESC")
    suspend fun findFavorites(): List<ReceiverConnectionProfileEntity>
}

@Dao
interface ReceiverCatalogDao {
    @Upsert suspend fun upsert(receiver: ReceiverCatalogEntity)
    @Query("SELECT * FROM receiver_catalog WHERE archivedAtEpochMillis IS NULL ORDER BY createdAtEpochMillis DESC") suspend fun findActive(): List<ReceiverCatalogEntity>
    @Query("SELECT * FROM receiver_catalog WHERE id = :id") suspend fun findById(id: String): ReceiverCatalogEntity?
    @Query("UPDATE receiver_catalog SET archivedAtEpochMillis = :archivedAtEpochMillis WHERE id = :id") suspend fun archive(id: String, archivedAtEpochMillis: Long)
}

@Dao
interface AntennaCatalogDao {
    @Upsert suspend fun upsert(antenna: AntennaCatalogEntity)
    @Query("SELECT * FROM antenna_catalog WHERE archivedAtEpochMillis IS NULL ORDER BY createdAtEpochMillis DESC") suspend fun findActive(): List<AntennaCatalogEntity>
    @Query("SELECT * FROM antenna_catalog WHERE id = :id") suspend fun findById(id: String): AntennaCatalogEntity?
    @Query("UPDATE antenna_catalog SET archivedAtEpochMillis = :archivedAtEpochMillis WHERE id = :id") suspend fun archive(id: String, archivedAtEpochMillis: Long)
}
