package br.f21campo.domain

/**
 * Immutable evidence captured for an occupation.
 *
 * These values deliberately copy the records used in the field instead of
 * pointing only at mutable catalogue rows.  A later edit to a station,
 * reference point, receiver or antenna therefore cannot rewrite history.
 */
data class StationSnapshot(
    val stationId: EntityId?,
    val name: String,
    val locality: String?,
    val municipality: String?,
)

data class ReferencePointSnapshot(
    val referencePointId: EntityId?,
    val type: ReferencePointType?,
    val code: String?,
    val description: String?,
    val observation: String?,
)

data class ReceiverSnapshot(
    val receiverId: EntityId?,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val firmware: String?,
    val source: ProvenanceSource = ProvenanceSource.OPERATOR,
)

data class AntennaSnapshot(
    val antennaId: EntityId?,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val source: ProvenanceSource = ProvenanceSource.OPERATOR,
)

data class OccupationSnapshots(
    val station: StationSnapshot? = null,
    val referencePoint: ReferencePointSnapshot? = null,
    val receiver: ReceiverSnapshot? = null,
    val antenna: AntennaSnapshot? = null,
)
