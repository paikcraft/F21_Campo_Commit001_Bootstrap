package br.f21campo.data

import br.f21campo.domain.DomainResult
import br.f21campo.domain.EntityId
import br.f21campo.domain.Project
import br.f21campo.domain.Station
import br.f21campo.domain.ReferencePoint
import br.f21campo.domain.Occupation
import br.f21campo.domain.EntityId
import java.time.Instant

class ProjectStationRepository(
    private val projectDao: ProjectDao,
    private val stationDao: StationDao,
    private val referencePointDao: ReferencePointDao? = null,
    private val occupationDao: OccupationDao? = null,
    private val artifactDao: OccupationArtifactDao? = null,
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

    suspend fun saveRawArtifact(occupationId: EntityId, path: String, sizeBytes: Long, sha256: String): DomainResult<Unit> {
        val dao = artifactDao ?: return DomainResult.Failure(br.f21campo.domain.DomainError.InvalidValue("artifact", "DAO not configured"))
        dao.upsert(OccupationArtifactEntity(EntityId.new().value, occupationId.value, "RAW_RECEIVER", path, sizeBytes, sha256, Instant.now().toEpochMilli()))
        return DomainResult.Success(Unit)
    }
}
