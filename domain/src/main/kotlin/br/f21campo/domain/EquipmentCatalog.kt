package br.f21campo.domain

import java.time.Instant

/** Operator-maintained catalog item. It is not a hardware detection result. */
data class ReceiverCatalogItem(
    val id: EntityId,
    val manufacturer: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val firmware: String? = null,
    val createdAt: Instant,
    val archivedAt: Instant? = null,
    val source: ProvenanceSource = ProvenanceSource.OPERATOR,
)

/** Operator-maintained catalog item. It is not a hardware detection result. */
data class AntennaCatalogItem(
    val id: EntityId,
    val manufacturer: String? = null,
    val model: String? = null,
    val serialNumber: String? = null,
    val createdAt: Instant,
    val archivedAt: Instant? = null,
    val source: ProvenanceSource = ProvenanceSource.OPERATOR,
)
