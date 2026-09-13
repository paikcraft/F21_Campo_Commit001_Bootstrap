package br.f21campo.data

/** Versioned, neutral envelope for sharing structured F-21 data. */
data class DatabaseExchangeEnvelope(
    val format: String = FORMAT,
    val formatVersion: Int = CURRENT_VERSION,
    val exportedAtEpochMillis: Long,
    val projects: List<ProjectEntity> = emptyList(),
    val stations: List<StationEntity> = emptyList(),
    val referencePoints: List<ReferencePointEntity> = emptyList(),
    val occupations: List<OccupationEntity> = emptyList(),
    val heightMeasurements: List<HeightMeasurementEntity> = emptyList(),
    val events: List<OccupationEventEntity> = emptyList(),
    val artifacts: List<OccupationArtifactEntity> = emptyList(),
    val receiverConnectionProfiles: List<ReceiverConnectionProfileEntity> = emptyList(),
) {
    fun validate(): Result<Unit> {
        if (format != FORMAT) return Result.failure(IllegalArgumentException("Formato de troca desconhecido"))
        if (formatVersion != CURRENT_VERSION) return Result.failure(IllegalArgumentException("Versão de troca não suportada: $formatVersion"))
        val duplicateIds = projects.map { it.id } + stations.map { it.id } + referencePoints.map { it.id } + occupations.map { it.id }
        if (duplicateIds.any { it.isBlank() }) return Result.failure(IllegalArgumentException("Registro sem ID"))
        if (duplicateIds.groupingBy { it }.eachCount().any { it.value > 1 }) {
            return Result.failure(IllegalArgumentException("IDs duplicados no conjunto de troca"))
        }
        return Result.success(Unit)
    }

    companion object {
        const val FORMAT = "f21-database-exchange"
        const val CURRENT_VERSION = 1
    }
}
