package br.f21campo.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val V1_TO_V2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS reference_points (id TEXT NOT NULL PRIMARY KEY, stationId TEXT NOT NULL, type TEXT NOT NULL, code TEXT NOT NULL, description TEXT, observation TEXT)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_reference_points_stationId ON reference_points(stationId)")
        }
    }

    val V2_TO_V3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS occupations (id TEXT NOT NULL PRIMARY KEY, projectId TEXT NOT NULL, stationId TEXT NOT NULL, state TEXT NOT NULL, plannedStartEpochMillis INTEGER, confirmedStartEpochMillis INTEGER, confirmedStopEpochMillis INTEGER, receiverModel TEXT, antennaModel TEXT)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_occupations_state ON occupations(state)")
        }
    }
    val V3_TO_V4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE occupations ADD COLUMN hasBeforeHeight INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE occupations ADD COLUMN beforeHeightMeters REAL")
        }
    }
}
