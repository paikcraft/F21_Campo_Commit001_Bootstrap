package br.f21campo.data

import androidx.room.withTransaction
import br.f21campo.domain.DomainResult
import br.f21campo.domain.EntityId
import br.f21campo.domain.Project
import br.f21campo.domain.Station
import br.f21campo.domain.ReferencePoint
import br.f21campo.domain.ReferencePointType
import br.f21campo.domain.ReceiverCatalogItem
import br.f21campo.domain.AntennaCatalogItem
import br.f21campo.domain.AuditEvent
import br.f21campo.domain.StoredFile
import br.f21campo.domain.ValidationState
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

data class DatabaseImportSummary(
    val projects: Int,
    val stations: Int,
    val referencePoints: Int,
    val occupations: Int,
    val heights: Int,
    val events: Int,
    val artifacts: Int,
    val connectionProfiles: Int,
    val conflicts: List<String> = emptyList(),
)

class ProjectStationRepository(
    private val projectDao: ProjectDao,
    private val stationDao: StationDao,
    private val referencePointDao: ReferencePointDao? = null,
    private val occupationDao: OccupationDao? = null,
    private val artifactDao: OccupationArtifactDao? = null,
    private val eventDao: OccupationEventDao? = null,
    private val heightDao: HeightMeasurementDao? = null,
    private val connectionProfileDao: ReceiverConnectionProfileDao? = null,
    private val database: F21Database? = null,
    private val receiverCatalogDao: ReceiverCatalogDao? = null,
    private val antennaCatalogDao: AntennaCatalogDao? = null,
    private val auditEventDao: AuditEventDao? = null,
) {
    suspend fun importCoreExchange(envelope: DatabaseExchangeEnvelope): Result<DatabaseImportSummary> {
        envelope.validate().getOrElse { return Result.failure(it) }
        val db = database ?: return Result.failure(IllegalStateException("Banco não configurado para importação"))
        return runCatching {
            val conflicts = buildList {
                envelope.projects.forEach { if (projectDao.findById(it.id) != null) add("project:${it.id}") }
                envelope.stations.forEach { if (stationDao.findById(it.id) != null) add("station:${it.id}") }
                envelope.referencePoints.forEach { if (referencePointDao?.findById(it.id) != null) add("referencePoint:${it.id}") }
                envelope.occupations.forEach { if (occupationDao?.findById(it.id) != null) add("occupation:${it.id}") }
            }
            db.withTransaction {
                projectDao.upsertAll(envelope.projects)
                stationDao.upsertAll(envelope.stations)
                referencePointDao?.upsertAll(envelope.referencePoints)
                    ?: error("DAO de referências não configurado")
                occupationDao?.upsertAll(envelope.occupations)
                    ?: error("DAO de ocupações não configurado")
                heightDao?.upsertAll(envelope.heightMeasurements)
                    ?: error("DAO de alturas não configurado")
                eventDao?.upsertAll(envelope.events)
                    ?: error("DAO de eventos não configurado")
                artifactDao?.upsertAll(envelope.artifacts)
                    ?: error("DAO de arquivos não configurado")
                connectionProfileDao?.upsertAll(envelope.receiverConnectionProfiles)
                    ?: error("DAO de perfis de conexão não configurado")
            }
            DatabaseImportSummary(envelope.projects.size, envelope.stations.size, envelope.referencePoints.size, envelope.occupations.size, envelope.heightMeasurements.size, envelope.events.size, envelope.artifacts.size, envelope.receiverConnectionProfiles.size, conflicts)
        }
    }
    /** Creates a validated, structured snapshot for future JSON export. No files are written here. */
    suspend fun createDatabaseExchangeEnvelope(): DatabaseExchangeEnvelope {
        val envelope = DatabaseExchangeEnvelope(
            exportedAtEpochMillis = Instant.now().toEpochMilli(),
            projects = projectDao.findAll().map { it },
            stations = stationDao.findAll().map { it },
            referencePoints = referencePointDao?.findAll().orEmpty(),
            occupations = occupationDao?.findAll().orEmpty(),
            heightMeasurements = heightDao?.findAll().orEmpty(),
            events = eventDao?.findAll().orEmpty(),
            artifacts = artifactDao?.findAll().orEmpty(),
            receiverConnectionProfiles = connectionProfileDao?.findAll().orEmpty(),
        )
        envelope.validate().getOrThrow()
        return envelope
    }

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
    suspend fun findReferencePointByStationTypeAndCode(stationId: EntityId, type: ReferencePointType, code: String): ReferencePoint? =
        referencePointDao?.findByStationTypeAndCode(stationId.value, type.name, code.trim())?.toDomain()

    suspend fun findAllProjects(includeArchived: Boolean = false): List<Project> =
        (if (includeArchived) projectDao.findAll() else projectDao.findActive()).map(ProjectEntity::toDomain)

    suspend fun findAllStations(includeArchived: Boolean = false): List<Station> =
        (if (includeArchived) stationDao.findAll() else stationDao.findActive()).map(StationEntity::toDomain)

    suspend fun findActiveProjectByName(name: String): Project? =
        projectDao.findActiveByName(name.trim())?.toDomain()

    suspend fun findActiveStationByIdentity(name: String, locality: String?): Station? =
        stationDao.findActiveByIdentity(name.trim(), locality?.trim()?.ifBlank { null })?.toDomain()

    suspend fun archiveProject(id: EntityId, archivedAt: Instant = Instant.now()): DomainResult<Unit> {
        projectDao.archive(id.value, archivedAt.toEpochMilli())
        return DomainResult.Success(Unit)
    }

    suspend fun archiveStation(id: EntityId, archivedAt: Instant = Instant.now()): DomainResult<Unit> {
        stationDao.archive(id.value, archivedAt.toEpochMilli())
        return DomainResult.Success(Unit)
    }

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
    suspend fun findOccupation(id: EntityId): Occupation? = occupationDao?.findById(id.value)?.toDomain()
    suspend fun findOccupationsByStation(stationId: EntityId): List<Occupation> = occupationDao?.findByStation(stationId.value)?.map(OccupationEntity::toDomain).orEmpty()

    suspend fun saveRawArtifact(occupationId: EntityId, path: String, sizeBytes: Long, sha256: String): DomainResult<Unit> {
        val dao = artifactDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("artifact", "DAO not configured"))
        if (dao.findByOccupationAndSha256(occupationId.value, sha256) != null) return DomainResult.Success(Unit)
        dao.upsert(OccupationArtifactEntity(EntityId.new().value, occupationId.value, "RAW_RECEIVER", path, sizeBytes, sha256, Instant.now().toEpochMilli()))
        return DomainResult.Success(Unit)
    }

    suspend fun hasRawArtifact(occupationId: EntityId): Boolean = artifactDao?.findByOccupation(occupationId.value)?.any { it.role == "RAW_RECEIVER" } == true
    suspend fun rawArtifactSummary(occupationId: EntityId): String? =
        artifactDao?.findByOccupation(occupationId.value)
            ?.lastOrNull { it.role == "RAW_RECEIVER" }
            ?.let { "${it.sha256.take(12)} · ${it.sizeBytes} bytes" }

    suspend fun findRawArtifacts(occupationId: EntityId): List<StoredFile> =
        artifactDao?.findByOccupation(occupationId.value)
            ?.filter { it.role == "RAW_RECEIVER" }
            ?.map { StoredFile(EntityId(it.id), it.path, it.sizeBytes, it.sha256, immutableOriginal = true, validation = ValidationState.UNKNOWN) }
            .orEmpty()

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
                id = EntityId(it.id),
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
        dao.upsert(HeightMeasurementEntity(measurement.id.value, occupationId.value, measurement.phase.name, measurement.valueMeters, measurement.type.name, measurement.observedAt?.toEpochMilli(), measurement.observation))
        return DomainResult.Success(Unit)
    }

    suspend fun replaceHeights(occupationId: EntityId, phase: HeightPhase, measurements: List<HeightMeasurement>): DomainResult<Unit> {
        val dao = heightDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("height", "DAO not configured"))
        val db = database
        if (db != null) {
            db.withTransaction {
                dao.deleteByOccupationAndPhase(occupationId.value, phase.name)
                measurements.filter { it.phase == phase && it.valueMeters.isFinite() }.forEach { measurement ->
                    dao.upsert(HeightMeasurementEntity(measurement.id.value, occupationId.value, measurement.phase.name, measurement.valueMeters, measurement.type.name, measurement.observedAt?.toEpochMilli(), measurement.observation))
                }
            }
        } else {
            dao.deleteByOccupationAndPhase(occupationId.value, phase.name)
            measurements.filter { it.phase == phase && it.valueMeters.isFinite() }.forEach { saveHeight(occupationId, it) }
        }
        return DomainResult.Success(Unit)
    }

    suspend fun saveConnectionProfile(profile: ReceiverConnectionProfile): DomainResult<Unit> {
        val dao = connectionProfileDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("connectionProfile", "DAO not configured"))
        dao.upsert(
            ReceiverConnectionProfileEntity(
                id = EntityId.new().value,
                receiverManufacturer = profile.receiverManufacturer,
                receiverModel = profile.receiverModel,
                receiverSerial = profile.receiverSerial,
                isFavorite = profile.isFavorite,
                transportType = profile.transportType.name,
                hostOrAddress = profile.hostOrAddress,
                port = profile.port,
                bluetoothName = profile.bluetoothName,
                bluetoothMac = profile.bluetoothMac,
                bluetoothServiceUuid = profile.bluetoothServiceUuid,
                notes = profile.notes,
                savedAtEpochMillis = Instant.now().toEpochMilli(),
            ),
        )
        return DomainResult.Success(Unit)
    }

    suspend fun findConnectionProfiles(): List<ReceiverConnectionProfile> =
        connectionProfileDao?.findAll()?.map {
            ReceiverConnectionProfile(
                receiverManufacturer = it.receiverManufacturer,
                receiverModel = it.receiverModel,
                receiverSerial = it.receiverSerial,
                isFavorite = it.isFavorite,
                transportType = ReceiverTransportType.valueOf(it.transportType),
                hostOrAddress = it.hostOrAddress,
                port = it.port,
                bluetoothName = it.bluetoothName,
                bluetoothMac = it.bluetoothMac,
                bluetoothServiceUuid = it.bluetoothServiceUuid,
                notes = it.notes,
            )
        }.orEmpty()

    suspend fun findFavoriteReceiverProfiles(): List<ReceiverConnectionProfile> =
        connectionProfileDao?.findFavorites()?.map {
            ReceiverConnectionProfile(
                receiverManufacturer = it.receiverManufacturer,
                receiverModel = it.receiverModel,
                receiverSerial = it.receiverSerial,
                isFavorite = it.isFavorite,
                transportType = ReceiverTransportType.valueOf(it.transportType),
                hostOrAddress = it.hostOrAddress,
                port = it.port,
                bluetoothName = it.bluetoothName,
                bluetoothMac = it.bluetoothMac,
                bluetoothServiceUuid = it.bluetoothServiceUuid,
                notes = it.notes,
            )
        }.orEmpty()

    suspend fun findActiveReceiverCatalog(): List<ReceiverCatalogItem> =
        receiverCatalogDao?.findActive()?.map(ReceiverCatalogEntity::toDomain).orEmpty()

    suspend fun findActiveAntennaCatalog(): List<AntennaCatalogItem> =
        antennaCatalogDao?.findActive()?.map(AntennaCatalogEntity::toDomain).orEmpty()

    suspend fun saveReceiverCatalog(item: ReceiverCatalogItem): DomainResult<Unit> {
        val dao = receiverCatalogDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("receiverCatalog", "DAO not configured"))
        dao.upsert(item.toEntity())
        return DomainResult.Success(Unit)
    }

    suspend fun saveAntennaCatalog(item: AntennaCatalogItem): DomainResult<Unit> {
        val dao = antennaCatalogDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("antennaCatalog", "DAO not configured"))
        dao.upsert(item.toEntity())
        return DomainResult.Success(Unit)
    }

    suspend fun saveAudit(event: AuditEvent): DomainResult<Unit> {
        val dao = auditEventDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("audit", "DAO not configured"))
        dao.insert(event.toEntity())
        return DomainResult.Success(Unit)
    }

    suspend fun findAuditEvents(entityId: EntityId): List<AuditEvent> =
        auditEventDao?.findByEntityId(entityId.value)?.map(AuditEventEntity::toDomain).orEmpty()

    suspend fun archiveReceiverCatalog(id: EntityId, archivedAt: Instant = Instant.now()): DomainResult<Unit> {
        val dao = receiverCatalogDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("receiverCatalog", "DAO not configured"))
        dao.archive(id.value, archivedAt.toEpochMilli())
        return DomainResult.Success(Unit)
    }

    suspend fun archiveAntennaCatalog(id: EntityId, archivedAt: Instant = Instant.now()): DomainResult<Unit> {
        val dao = antennaCatalogDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("antennaCatalog", "DAO not configured"))
        dao.archive(id.value, archivedAt.toEpochMilli())
        return DomainResult.Success(Unit)
    }
}
