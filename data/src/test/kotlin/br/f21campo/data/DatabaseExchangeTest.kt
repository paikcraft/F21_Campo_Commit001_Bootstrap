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

    @Test
    fun completeEnvelopeWithNullableCoreRecordsPassesValidation() {
        val envelope = DatabaseExchangeEnvelope(
            exportedAtEpochMillis = 10L,
            projects = listOf(ProjectEntity("p1", "Comissão 1", 1L, null)),
            stations = listOf(StationEntity("s1", "Estação A", "Manaus", null, 2L, null)),
            referencePoints = listOf(ReferencePointEntity("r1", "s1", "RN", "RN-01", null, "campo")),
            occupations = listOf(OccupationEntity("o1", "p1", "s1", "r1", 1200L, "STOPPED", 3L, 4L, 5L, "S900", "ASH801", "Spectra", "Spectra", "rx", "ant", true, 1.234)),
            heightMeasurements = listOf(HeightMeasurementEntity("h1", "o1", "BEFORE", 1.234, "VERTICAL", 6L, null)),
            events = listOf(OccupationEventEntity("e1", "o1", 7L, "NOTE", "INFO", "ok", "OPERATOR")),
            artifacts = listOf(OccupationArtifactEntity("a1", "o1", "RAW_RECEIVER", "raw.bin", 8L, "abc", 9L)),
            receiverConnectionProfiles = listOf(ReceiverConnectionProfileEntity("c1", "Spectra", "S900", null, true, "WIFI_TCP", "192.0.2.1", 2101, null, null, null, "manual", 10L)),
        )

        assertTrue(envelope.validate().isSuccess)
    }
}
