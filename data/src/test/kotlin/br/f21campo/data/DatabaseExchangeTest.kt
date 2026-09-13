package br.f21campo.data

import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseExchangeTest {
    @Test
    fun validEnvelopePassesValidation() {
        val result = DatabaseExchangeEnvelope(exportedAtEpochMillis = 1L).validate()
        assertTrue(result.isSuccess)
    }

    @Test
    fun unknownFormatFailsValidation() {
        val result = DatabaseExchangeEnvelope(format = "other", exportedAtEpochMillis = 1L).validate()
        assertTrue(result.isFailure)
    }

    @Test
    fun duplicateIdsFailValidation() {
        val project = ProjectEntity("same", "Projeto", 1L, null)
        val result = DatabaseExchangeEnvelope(exportedAtEpochMillis = 1L, projects = listOf(project, project)).validate()
        assertTrue(result.isFailure)
    }
}
