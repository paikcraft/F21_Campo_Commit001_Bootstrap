package br.f21campo.domain

/**
 * UI-neutral checklist used before a manual occupation can be marked READY.
 * Names are kept as input because the domain does not infer or fabricate them.
 */
data class ReadinessInput(
    val projectName: String,
    val stationName: String,
    val locality: String,
    val hasReference: Boolean,
    val hasEquipment: Boolean,
    val hasBeforeHeight: Boolean,
)

object OccupationReadiness {
    fun missing(input: ReadinessInput): List<String> = buildList {
        if (input.projectName.isBlank()) add("projeto/LH")
        if (input.stationName.isBlank()) add("nome da estação")
        if (input.locality.isBlank()) add("localidade")
        if (!input.hasReference) add("referência RN/MT/PA")
        if (!input.hasEquipment) add("equipamento")
        if (!input.hasBeforeHeight) add("altura BEFORE")
    }
}
