package br.f21campo.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.f21campo.domain.Antenna
import br.f21campo.domain.AuditEvent
import br.f21campo.domain.DomainResult
import br.f21campo.domain.EntityId
import br.f21campo.domain.EventSeverity
import br.f21campo.domain.HeightMeasurement
import br.f21campo.domain.HeightPhase
import br.f21campo.domain.HeightType
import br.f21campo.domain.ManualEquipment
import br.f21campo.domain.Occupation
import br.f21campo.domain.OccupationEvent
import br.f21campo.domain.OccupationEventCategory
import br.f21campo.domain.OccupationState
import br.f21campo.domain.OccupationStateMachine
import br.f21campo.domain.ProvenanceSource
import br.f21campo.domain.Project
import br.f21campo.domain.Receiver
import br.f21campo.domain.ReferencePoint
import br.f21campo.domain.ReferencePointType
import br.f21campo.domain.Station
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GateR2EndToEndTest {
    @Test
    fun manualFieldFlowPersistsAndReopensAllEvidence() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "gate-r2-${System.currentTimeMillis()}.db"
        val first = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        val repository = repository(first)
        val project = Project(EntityId("project-r2"), "LH Manaus", Instant.ofEpochMilli(1L))
        val station = Station(EntityId("station-r2"), "RN 01", "Manaus", null, Instant.ofEpochMilli(2L))
        val reference = ReferencePoint(EntityId("reference-r2"), station.id, ReferencePointType.RN, "RN-01")
        repository.save(project)
        repository.save(station)
        repository.save(reference)

        var occupation = Occupation(EntityId("occupation-r2"), project.id, station.id, reference.id, plannedDurationSeconds = 1200L)
        assertTrue(repository.save(occupation) is DomainResult.Success)
        occupation = (ManualEquipment.attachSnapshot(
            occupation,
            Receiver(EntityId("receiver-r2"), "Spectra", "S900", "rx-r2", ProvenanceSource.OPERATOR, "fw-r2"),
            Antenna(EntityId("antenna-r2"), "Spectra", "ASH801", "ant-r2"),
        ) as DomainResult.Success).value
        repository.save(occupation)
        repository.save(station.copy(name = "RN 01 editada depois"))
        repository.save(occupation)

        repository.replaceHeights(
            occupation.id,
            HeightPhase.BEFORE,
            listOf(
                HeightMeasurement(HeightPhase.BEFORE, 1.234, HeightType.VERTICAL, Instant.ofEpochMilli(3L), "unit=mm", EntityId("before-r2-1")),
                HeightMeasurement(HeightPhase.BEFORE, 1.236, HeightType.VERTICAL, Instant.ofEpochMilli(4L), "unit=mm", EntityId("before-r2-2")),
            ),
        )
        occupation = occupation.copy(hasBeforeHeight = true, beforeHeightMeters = 1.234)
        occupation = (OccupationStateMachine.ready(occupation) as DomainResult.Success).value
        repository.save(occupation)
        occupation = (OccupationStateMachine.start(occupation, Instant.ofEpochMilli(5L)) as DomainResult.Success).value
        repository.save(occupation)
        repository.save(OccupationEvent(EntityId("event-r2"), occupation.id, Instant.ofEpochMilli(6L), OccupationEventCategory.WEATHER, EventSeverity.WARNING, "chuva", ProvenanceSource.OPERATOR))
        occupation = (OccupationStateMachine.stop(occupation, Instant.ofEpochMilli(7L)) as DomainResult.Success).value
        repository.save(occupation)
        repository.replaceHeights(
            occupation.id,
            HeightPhase.AFTER,
            listOf(HeightMeasurement(HeightPhase.AFTER, 1.235, HeightType.VERTICAL, Instant.ofEpochMilli(8L), "unit=mm", EntityId("after-r2-1"))),
        )
        repository.saveRawArtifact(occupation.id, "/controlled/r2.raw", 12L, "sha-r2")
        repository.saveAudit(AuditEvent(EntityId("audit-r2"), Instant.ofEpochMilli(9L), "RAW_IMPORTED_SHA256", "operator", occupation.id))
        occupation = (OccupationStateMachine.collectWithEvidence(occupation, true, true) as DomainResult.Success).value
        repository.save(occupation)
        occupation = (OccupationStateMachine.validate(occupation) as DomainResult.Success).value
        repository.save(occupation)
        first.close()

        val second = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        val reopened = repository(second).findOccupation(occupation.id)
        assertNotNull(reopened)
        assertEquals(OccupationState.VALIDATED, reopened?.state)
        assertEquals("RN-01", second.referencePointDao().findById(reference.id.value)?.code)
        assertEquals("S900", reopened?.equipment?.receiver?.model)
        assertEquals("fw-r2", reopened?.equipment?.receiver?.firmware)
        assertEquals("RN-01", reopened?.snapshots?.referencePoint?.code)
        assertEquals("RN 01", reopened?.snapshots?.station?.name)
        assertEquals("ASH801", reopened?.snapshots?.antenna?.model)
        assertEquals(2, repository(second).findHeights(occupation.id).count { it.phase == HeightPhase.BEFORE })
        assertEquals(1, repository(second).findHeights(occupation.id).count { it.phase == HeightPhase.AFTER })
        assertEquals("sha-r2", second.occupationArtifactDao().findByOccupation(occupation.id.value).single().sha256)
        assertEquals("chuva", repository(second).findEvents(occupation.id).single().description)
        assertEquals("RAW_IMPORTED_SHA256", repository(second).findAuditEvents(occupation.id).single().action)
        second.close()
        context.deleteDatabase(name)
    }

    @Test
    fun activeOccupationSurvivesCloseAndReopenForContinueFlow() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "gate-r2-active-${System.currentTimeMillis()}.db"
        val first = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        val firstRepository = repository(first)
        val project = Project(EntityId("project-active"), "LH active", Instant.ofEpochMilli(11L))
        val station = Station(EntityId("station-active"), "RN active", "Manaus", null, Instant.ofEpochMilli(12L))
        val reference = ReferencePoint(EntityId("reference-active"), station.id, ReferencePointType.RN, "RN-ACTIVE")
        firstRepository.save(project)
        firstRepository.save(station)
        firstRepository.save(reference)
        var occupation = Occupation(EntityId("occupation-active"), project.id, station.id, reference.id, hasBeforeHeight = true, beforeHeightMeters = 1.2)
        occupation = (ManualEquipment.attachSnapshot(
            occupation,
            Receiver(EntityId("receiver-active"), "Spectra", "S900", "rx-active", ProvenanceSource.OPERATOR, "fw-active"),
            Antenna(EntityId("antenna-active"), "Spectra", "ASH801", "ant-active"),
        ) as DomainResult.Success).value
        occupation = (OccupationStateMachine.ready(occupation) as DomainResult.Success).value
        occupation = (OccupationStateMachine.start(occupation, Instant.ofEpochMilli(20L)) as DomainResult.Success).value
        firstRepository.save(occupation)
        firstRepository.replaceHeights(
            occupation.id,
            HeightPhase.BEFORE,
            listOf(HeightMeasurement(HeightPhase.BEFORE, 1.2, HeightType.VERTICAL, Instant.ofEpochMilli(19L), "unit=m", EntityId("height-active"))),
        )
        firstRepository.save(OccupationEvent(EntityId("event-active"), occupation.id, Instant.ofEpochMilli(21L), OccupationEventCategory.CONNECTION, EventSeverity.INFO, "canal preparado", ProvenanceSource.OPERATOR))
        first.close()

        val second = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        val recovered = repository(second).findIncompleteOccupations().single()
        assertEquals(OccupationState.ACTIVE, recovered.state)
        assertEquals(20L, recovered.confirmedStart?.toEpochMilli())
        assertEquals(project.id, recovered.projectId)
        assertEquals(station.id, recovered.stationId)
        assertEquals("RN-ACTIVE", recovered.snapshots?.referencePoint?.code)
        assertEquals("S900", recovered.snapshots?.receiver?.model)
        assertEquals(1, repository(second).findEvents(recovered.id).size)
        assertEquals(1, repository(second).findHeights(recovered.id).size)
        second.close()
        context.deleteDatabase(name)
    }

    private fun repository(database: F21Database) = ProjectStationRepository(
        database.projectDao(),
        database.stationDao(),
        database.referencePointDao(),
        database.occupationDao(),
        database.occupationArtifactDao(),
        database.occupationEventDao(),
        database.heightMeasurementDao(),
        database.receiverConnectionProfileDao(),
        database,
        database.receiverCatalogDao(),
        database.antennaCatalogDao(),
        database.auditEventDao(),
        database.occupationSnapshotDao(),
    )
}
