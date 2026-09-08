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
}
