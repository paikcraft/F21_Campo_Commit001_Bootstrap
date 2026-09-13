package br.f21campo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OccupationReadinessTest {
    @Test
    fun emptyChecklistReportsEveryRequiredField() {
        val missing = OccupationReadiness.missing(ReadinessInput("", "", "", false, false, false))
        assertEquals(
            listOf("projeto/LH", "nome da estação", "localidade", "referência RN/MT/PA", "equipamento", "altura antes do rastreio"),
            missing,
        )
    }

    @Test
    fun completeChecklistHasNoPendingItems() {
        assertTrue(OccupationReadiness.missing(ReadinessInput("LH 01", "Estação A", "Manaus", true, true, true)).isEmpty())
    }
}
