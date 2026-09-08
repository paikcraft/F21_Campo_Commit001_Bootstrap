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
    val V4_TO_V5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE occupations ADD COLUMN referencePointId TEXT")
        }
    }
    val V5_TO_V6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS occupation_artifacts (id TEXT NOT NULL PRIMARY KEY, occupationId TEXT NOT NULL, role TEXT NOT NULL, path TEXT NOT NULL, sizeBytes INTEGER NOT NULL, sha256 TEXT NOT NULL, importedAtEpochMillis INTEGER NOT NULL)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_occupation_artifacts_occupationId ON occupation_artifacts(occupationId)")
        }
    }
    val V6_TO_V7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS occupation_events (id TEXT NOT NULL PRIMARY KEY, occupationId TEXT NOT NULL, atEpochMillis INTEGER NOT NULL, category TEXT NOT NULL, severity TEXT NOT NULL, description TEXT NOT NULL, source TEXT NOT NULL)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_occupation_events_occupationId ON occupation_events(occupationId)")
        }
    }
    val V7_TO_V8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE occupations ADD COLUMN plannedDurationSeconds INTEGER NOT NULL DEFAULT 1200")
        }
    }
    val V8_TO_V9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE occupations ADD COLUMN plannedDurationSeconds_new INTEGER")
            database.execSQL("UPDATE occupations SET plannedDurationSeconds_new = plannedDurationSeconds")
            database.execSQL("CREATE TABLE occupations_new (id TEXT NOT NULL PRIMARY KEY, projectId TEXT NOT NULL, stationId TEXT NOT NULL, referencePointId TEXT, plannedDurationSeconds INTEGER, state TEXT NOT NULL, plannedStartEpochMillis INTEGER, confirmedStartEpochMillis INTEGER, confirmedStopEpochMillis INTEGER, receiverModel TEXT, antennaModel TEXT, hasBeforeHeight INTEGER NOT NULL, beforeHeightMeters REAL)")
            database.execSQL("INSERT INTO occupations_new SELECT id, projectId, stationId, referencePointId, plannedDurationSeconds_new, state, plannedStartEpochMillis, confirmedStartEpochMillis, confirmedStopEpochMillis, receiverModel, antennaModel, hasBeforeHeight, beforeHeightMeters FROM occupations")
            database.execSQL("DROP TABLE occupations")
            database.execSQL("ALTER TABLE occupations_new RENAME TO occupations")
        }
    }
}
