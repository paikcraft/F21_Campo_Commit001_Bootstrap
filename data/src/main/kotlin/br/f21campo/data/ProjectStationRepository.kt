package br.f21campo.data

import br.f21campo.domain.DomainResult
import br.f21campo.domain.EntityId
import br.f21campo.domain.Project
import br.f21campo.domain.Station
import br.f21campo.domain.ReferencePoint
import br.f21campo.domain.Occupation
import br.f21campo.domain.OccupationEvent
import br.f21campo.domain.HeightMeasurement
import br.f21campo.domain.HeightPhase
import br.f21campo.domain.HeightType
import br.f21campo.domain.OccupationEventCategory
import br.f21campo.domain.EventSeverity
import br.f21campo.domain.ProvenanceSource
import br.f21campo.receiver.api.ReceiverConnectionProfile
import br.f21campo.receiver.api.ReceiverTransportType
import java.time.Instant

class ProjectStationRepository(
    private val projectDao: ProjectDao,
    private val stationDao: StationDao,
    private val referencePointDao: ReferencePointDao? = null,
    private val occupationDao: OccupationDao? = null,
    private val artifactDao: OccupationArtifactDao? = null,
    private val eventDao: OccupationEventDao? = null,
    private val heightDao: HeightMeasurementDao? = null,
    private val connectionProfileDao: ReceiverConnectionProfileDao? = null,
) {
    suspend fun save(project: Project): DomainResult<Unit> {
        projectDao.upsert(project.toEntity())
        return DomainResult.Success(Unit)
    }

    suspend fun save(station: Station): DomainResult<Unit> {
        stationDao.upsert(station.toEntity())
        return DomainResult.Success(Unit)
    }

    suspend fun findProject(id: EntityId): Project? = projectDao.findById(id.value)?.toDomain()
    suspend fun findStation(id: EntityId): Station? = stationDao.findById(id.value)?.toDomain()
    suspend fun findReferencePoint(id: EntityId): ReferencePoint? = referencePointDao?.findById(id.value)?.toDomain()
    suspend fun findReferencePointsByStation(stationId: EntityId): List<ReferencePoint> =
        referencePointDao?.findByStation(stationId.value)?.map(ReferencePointEntity::toDomain).orEmpty()
    suspend fun findAllProjects(): List<Project> = projectDao.findAll().map(ProjectEntity::toDomain)
    suspend fun findAllStations(): List<Station> = stationDao.findAll().map(StationEntity::toDomain)

    suspend fun save(referencePoint: ReferencePoint): DomainResult<Unit> {
        val dao = referencePointDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("referencePoint", "DAO not configured"))
        dao.upsert(ReferencePointEntity(referencePoint.id.value, referencePoint.stationId.value, referencePoint.type.name, referencePoint.code, referencePoint.description, referencePoint.observation))
        return DomainResult.Success(Unit)
    }

    suspend fun save(occupation: Occupation): DomainResult<Unit> {
        val dao = occupationDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("occupation", "DAO not configured"))
        dao.upsert(occupation.toEntity())
        return DomainResult.Success(Unit)
    }

    suspend fun findIncompleteOccupations(): List<Occupation> = occupationDao?.findIncomplete()?.map(OccupationEntity::toDomain).orEmpty()
    suspend fun findOccupationsByStation(stationId: EntityId): List<Occupation> = occupationDao?.findByStation(stationId.value)?.map(OccupationEntity::toDomain).orEmpty()

    suspend fun saveRawArtifact(occupationId: EntityId, path: String, sizeBytes: Long, sha256: String): DomainResult<Unit> {
        val dao = artifactDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("artifact", "DAO not configured"))
        dao.upsert(OccupationArtifactEntity(EntityId.new().value, occupationId.value, "RAW_RECEIVER", path, sizeBytes, sha256, Instant.now().toEpochMilli()))
        return DomainResult.Success(Unit)
    }

    suspend fun hasRawArtifact(occupationId: EntityId): Boolean = artifactDao?.findByOccupation(occupationId.value)?.any { it.role == "RAW_RECEIVER" } == true
    suspend fun rawArtifactSummary(occupationId: EntityId): String? =
        artifactDao?.findByOccupation(occupationId.value)
            ?.lastOrNull { it.role == "RAW_RECEIVER" }
            ?.let { "${it.sha256.take(12)} · ${it.sizeBytes} bytes" }

    suspend fun hasHeight(occupationId: EntityId, phase: HeightPhase): Boolean =
        heightDao?.findByOccupation(occupationId.value)?.any { it.phase == phase.name } == true

    suspend fun findHeights(occupationId: EntityId): List<HeightMeasurement> =
        heightDao?.findByOccupation(occupationId.value)?.map {
            HeightMeasurement(
                phase = HeightPhase.valueOf(it.phase),
                valueMeters = it.valueMeters,
                type = HeightType.valueOf(it.type),
                observedAt = it.observedAtEpochMillis?.let(Instant::ofEpochMilli),
                observation = it.observation,
            )
        }.orEmpty()

    suspend fun save(event: OccupationEvent): DomainResult<Unit> {
        val dao = eventDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("event", "DAO not configured"))
        dao.upsert(OccupationEventEntity(event.id.value, event.occupationId.value, event.at.toEpochMilli(), event.category.name, event.severity.name, event.description, event.source.name))
        return DomainResult.Success(Unit)
    }

    /** Field events are persisted evidence and can be recovered with the occupation. */
    suspend fun findEvents(occupationId: EntityId): List<OccupationEvent> =
        eventDao?.findByOccupation(occupationId.value)?.map {
            OccupationEvent(
                id = EntityId(it.id),
                occupationId = EntityId(it.occupationId),
                at = Instant.ofEpochMilli(it.atEpochMillis),
                category = OccupationEventCategory.valueOf(it.category),
                severity = EventSeverity.valueOf(it.severity),
                description = it.description,
                source = ProvenanceSource.valueOf(it.source),
            )
        }.orEmpty()

    suspend fun saveHeight(occupationId: EntityId, measurement: HeightMeasurement): DomainResult<Unit> {
        val dao = heightDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("height", "DAO not configured"))
        dao.upsert(HeightMeasurementEntity(EntityId.new().value, occupationId.value, measurement.phase.name, measurement.valueMeters, measurement.type.name, measurement.observedAt?.toEpochMilli(), measurement.observation))
        return DomainResult.Success(Unit)
    }

    suspend fun saveConnectionProfile(profile: ReceiverConnectionProfile): DomainResult<Unit> {
        val dao = connectionProfileDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("connectionProfile", "DAO not configured"))
        dao.upsert(
            ReceiverConnectionProfileEntity(
                id = EntityId.new().value,
                transportType = profile.transportType.name,
                hostOrAddress = profile.hostOrAddress,
                port = profile.port,
                bluetoothName = profile.bluetoothName,
                bluetoothMac = profile.bluetoothMac,
                notes = profile.notes,
                savedAtEpochMillis = Instant.now().toEpochMilli(),
            ),
        )
        return DomainResult.Success(Unit)
    }

    suspend fun findConnectionProfiles(): List<ReceiverConnectionProfile> =
        connectionProfileDao?.findAll()?.map {
            ReceiverConnectionProfile(
                transportType = ReceiverTransportType.valueOf(it.transportType),
                hostOrAddress = it.hostOrAddress,
                port = it.port,
                bluetoothName = it.bluetoothName,
                bluetoothMac = it.bluetoothMac,
                notes = it.notes,
            )
        }.orEmpty()
}
