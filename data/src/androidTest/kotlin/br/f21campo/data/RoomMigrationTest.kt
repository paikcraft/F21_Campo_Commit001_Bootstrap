package br.f21campo.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import kotlinx.coroutines.runBlocking
import br.f21campo.domain.EntityId
import br.f21campo.domain.HeightMeasurement
import br.f21campo.domain.HeightPhase
import br.f21campo.domain.HeightType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        F21Database::class.java,
        emptyList(),
    )

    @Test
    @Throws(IOException::class)
    fun migrateV1ToCurrentPreservesProjectAndStation() {
        helper.createDatabase("migration-test", 1).apply {
            execSQL("CREATE TABLE projects (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, archivedAtEpochMillis INTEGER)")
            execSQL("CREATE TABLE stations (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, locality TEXT, municipality TEXT, createdAtEpochMillis INTEGER NOT NULL, archivedAtEpochMillis INTEGER)")
            execSQL("INSERT INTO projects VALUES ('p1', 'Comissão 1', 1, NULL)")
            execSQL("INSERT INTO stations VALUES ('s1', 'Estação A', 'Manaus', NULL, 2, NULL)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate("migration-test", 17, true, *Migrations.ALL)
        migrated.query("SELECT name FROM projects WHERE id = 'p1'").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("Comissão 1", cursor.getString(0))
        }
        migrated.query("SELECT municipality FROM stations WHERE id = 's1'").use { cursor ->
            check(cursor.moveToFirst())
            check(cursor.isNull(0))
        }
        migrated.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'receiver_catalog'").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("receiver_catalog", cursor.getString(0))
        }
        migrated.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'antenna_catalog'").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("antenna_catalog", cursor.getString(0))
        }
        migrated.query("PRAGMA table_info(occupations)").use { cursor ->
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == "receiverFirmware") found = true
            }
            assertEquals(true, found)
        }
        migrated.close()
    }

    @Test
    fun currentDatabaseCanCloseAndReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "reopen-${System.currentTimeMillis()}.db"
        val first = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        first.projectDao().upsert(ProjectEntity("p-reopen", "Reabertura", 1L, null))
        first.close()

        val second = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        assertEquals("Reabertura", second.projectDao().findById("p-reopen")?.name)
        second.close()
        context.deleteDatabase(name)
    }

    @Test
    fun projectStationAndReferencePointKeepIdentityAndArchiveWithoutDelete() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "catalog-${System.currentTimeMillis()}.db"
        val database = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        database.projectDao().upsert(ProjectEntity("project-1", "LH Manaus", 10L, null))
        database.stationDao().upsert(StationEntity("station-1", "RN 01", "Manaus", null, 11L, null))
        database.referencePointDao().upsert(ReferencePointEntity("reference-1", "station-1", "RN", "RN-01", null, null))

        assertEquals("project-1", database.projectDao().findActiveByName("lh manaus")?.id)
        assertEquals("station-1", database.stationDao().findActiveByIdentity("RN 01", "Manaus")?.id)
        assertEquals("reference-1", database.referencePointDao().findByStationTypeAndCode("station-1", "RN", "RN-01")?.id)

        database.stationDao().archive("station-1", 20L)
        assertNull(database.stationDao().findActiveByIdentity("RN 01", "Manaus"))
        assertNotNull(database.stationDao().findById("station-1"))

        database.receiverCatalogDao().upsert(
            ReceiverCatalogEntity("receiver-1", "Spectra", "S900", "rx-1", "fw-1", 21L, null),
        )
        database.antennaCatalogDao().upsert(
            AntennaCatalogEntity("antenna-1", "Spectra", "ASH801", "ant-1", 22L, null),
        )
        assertEquals("S900", database.receiverCatalogDao().findActive().single().model)
        assertEquals("ASH801", database.antennaCatalogDao().findActive().single().model)
        database.receiverCatalogDao().archive("receiver-1", 23L)
        database.antennaCatalogDao().archive("antenna-1", 24L)
        assertEquals(0, database.receiverCatalogDao().findActive().size)
        assertEquals(0, database.antennaCatalogDao().findActive().size)
        assertNotNull(database.receiverCatalogDao().findById("receiver-1"))
        assertNotNull(database.antennaCatalogDao().findById("antenna-1"))
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun occupationEvidenceSurvivesCloseAndReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "occupation-reopen-${System.currentTimeMillis()}.db"
        val first = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        first.projectDao().upsert(ProjectEntity("p1", "LH 1", 1L, null))
        first.stationDao().upsert(StationEntity("s1", "RN 1", "Manaus", null, 2L, null))
        first.referencePointDao().upsert(ReferencePointEntity("r1", "s1", "RN", "RN-1", null, null))
        first.occupationDao().upsert(
            OccupationEntity(
                id = "o1",
                projectId = "p1",
                stationId = "s1",
                referencePointId = "r1",
                plannedDurationSeconds = 1200L,
                state = "ACTIVE",
                plannedStartEpochMillis = 3L,
                confirmedStartEpochMillis = 4L,
                confirmedStopEpochMillis = null,
                receiverModel = "S900",
                antennaModel = "ASH801",
                receiverManufacturer = "Spectra",
                antennaManufacturer = "Spectra",
                receiverSerial = "rx-1",
                antennaSerial = "ant-1",
                receiverFirmware = "fw-1",
                hasBeforeHeight = true,
                beforeHeightMeters = 1.234,
            ),
        )
        first.heightMeasurementDao().upsert(HeightMeasurementEntity("h1", "o1", "BEFORE", 1.234, "VERTICAL", 5L, "unit=mm"))
        first.occupationEventDao().upsert(OccupationEventEntity("e1", "o1", 6L, "NOTE", "INFO", "observado", "OPERATOR"))
        first.occupationArtifactDao().upsert(OccupationArtifactEntity("a1", "o1", "RAW_RECEIVER", "/raw/a1", 7L, "sha-a1", 8L))
        first.close()

        val second = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        val occupation = second.occupationDao().findById("o1")
        assertEquals("ACTIVE", occupation?.state)
        assertEquals("r1", occupation?.referencePointId)
        assertEquals("S900", occupation?.receiverModel)
        assertEquals("fw-1", occupation?.receiverFirmware)
        assertEquals(1.234, occupation?.beforeHeightMeters ?: 0.0, 0.000001)
        assertEquals(1, second.heightMeasurementDao().findByOccupation("o1").size)
        assertEquals("observado", second.occupationEventDao().findByOccupation("o1").single().description)
        assertEquals("sha-a1", second.occupationArtifactDao().findByOccupation("o1").single().sha256)
        second.close()
        context.deleteDatabase(name)
    }

    @Test
    fun equipmentCatalogEditsDoNotRewriteOccupationSnapshot() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "equipment-snapshot-${System.currentTimeMillis()}.db"
        val database = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        database.receiverCatalogDao().upsert(
            ReceiverCatalogEntity("receiver-1", "Spectra", "S900", "rx-1", "fw-1", 1L, null),
        )
        database.antennaCatalogDao().upsert(
            AntennaCatalogEntity("antenna-1", "Spectra", "ASH801", "ant-1", 2L, null),
        )
        database.occupationDao().upsert(
            OccupationEntity(
                id = "occupation-1",
                projectId = "project-1",
                stationId = "station-1",
                referencePointId = null,
                plannedDurationSeconds = null,
                state = "DRAFT",
                plannedStartEpochMillis = null,
                confirmedStartEpochMillis = null,
                confirmedStopEpochMillis = null,
                receiverModel = "S900",
                antennaModel = "ASH801",
                receiverManufacturer = "Spectra",
                antennaManufacturer = "Spectra",
                receiverSerial = "rx-1",
                antennaSerial = "ant-1",
                receiverFirmware = "fw-1",
                hasBeforeHeight = false,
                beforeHeightMeters = null,
            ),
        )
        database.receiverCatalogDao().upsert(
            ReceiverCatalogEntity("receiver-1", "Other", "R2", "rx-2", "fw-2", 1L, null),
        )
        database.antennaCatalogDao().archive("antenna-1", 3L)
        val snapshot = database.occupationDao().findById("occupation-1")
        assertEquals("S900", snapshot?.receiverModel)
        assertEquals("ASH801", snapshot?.antennaModel)
        assertEquals("rx-1", snapshot?.receiverSerial)
        assertEquals("ant-1", snapshot?.antennaSerial)
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun variableHeightSetCanBeReplacedByPhaseWithoutFixedCount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "height-replace-${System.currentTimeMillis()}.db"
        val database = Room.databaseBuilder(context, F21Database::class.java, name)
            .addMigrations(*Migrations.ALL)
            .build()
        val repository = ProjectStationRepository(
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
        )
        val occupationId = EntityId("occupation-height")
        repository.replaceHeights(
            occupationId,
            HeightPhase.BEFORE,
            listOf(
                HeightMeasurement(HeightPhase.BEFORE, 1.234, HeightType.VERTICAL, id = EntityId("h-before-1")),
                HeightMeasurement(HeightPhase.BEFORE, 1.236, HeightType.VERTICAL, id = EntityId("h-before-2")),
            ),
        )
        assertEquals(2, repository.findHeights(occupationId).size)
        repository.replaceHeights(
            occupationId,
            HeightPhase.BEFORE,
            listOf(HeightMeasurement(HeightPhase.BEFORE, 1.235, HeightType.SLANT, id = EntityId("h-before-new"))),
        )
        val current = repository.findHeights(occupationId)
        assertEquals(1, current.size)
        assertEquals(HeightType.SLANT, current.single().type)
        assertEquals("h-before-new", current.single().id.value)
        database.close()
        context.deleteDatabase(name)
    }
}
