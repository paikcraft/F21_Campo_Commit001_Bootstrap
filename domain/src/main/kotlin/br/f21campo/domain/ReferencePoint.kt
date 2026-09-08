package br.f21campo.domain

data class ReferencePoint(
    val id: EntityId,
    val stationId: EntityId,
    val type: ReferencePointType,
    val code: String,
    val description: String? = null,
    val observation: String? = null,
)

enum class ReferencePointType { RN, MT, PA }

enum class ArtifactRole { RAW_RECEIVER, RINEX_OBS, RINEX_NAV, PROCESSING_RESULT, PPP_REPORT, F21_PDF, OTHER }

data class HeightMeasurement(
    val phase: HeightPhase,
    val valueMeters: Double,
    val type: HeightType,
    val observedAt: java.time.Instant? = null,
    val observation: String? = null,
) {
    init { require(valueMeters.isFinite()) { "height must be finite" } }
}

data class HeightMeasurementSet(val measurements: List<HeightMeasurement>) {
    init { require(measurements.isNotEmpty()) { "at least one measurement is required" } }
    fun forPhase(phase: HeightPhase): List<HeightMeasurement> = measurements.filter { it.phase == phase }
    fun mean(phase: HeightPhase): Double? = forPhase(phase).map { it.valueMeters }.takeIf { it.isNotEmpty() }?.average()
    fun amplitude(phase: HeightPhase): Double? = forPhase(phase).map { it.valueMeters }.takeIf { it.isNotEmpty() }?.let { it.max() - it.min() }
    val deltaMean: Double? get() = mean(HeightPhase.AFTER)?.let { after -> mean(HeightPhase.BEFORE)?.let { after - it } }
}
