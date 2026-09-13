package br.f21campo.data

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

        val migrated = helper.runMigrationsAndValidate("migration-test", 15, true, *Migrations.ALL)
        migrated.query("SELECT name FROM projects WHERE id = 'p1'").use { cursor ->
            check(cursor.moveToFirst())
            assertEquals("Comissão 1", cursor.getString(0))
        }
        migrated.query("SELECT municipality FROM stations WHERE id = 's1'").use { cursor ->
            check(cursor.moveToFirst())
            check(cursor.isNull(0))
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
}
