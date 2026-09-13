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
    val V9_TO_V10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS height_measurements (id TEXT NOT NULL PRIMARY KEY, occupationId TEXT NOT NULL, phase TEXT NOT NULL, valueMeters REAL NOT NULL, type TEXT NOT NULL, observedAtEpochMillis INTEGER, observation TEXT)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_height_measurements_occupationId ON height_measurements(occupationId)")
        }
    }
    val V10_TO_V11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE occupations ADD COLUMN receiverSerial TEXT")
            database.execSQL("ALTER TABLE occupations ADD COLUMN antennaSerial TEXT")
        }
    }
    val V11_TO_V12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE occupations ADD COLUMN receiverManufacturer TEXT")
            database.execSQL("ALTER TABLE occupations ADD COLUMN antennaManufacturer TEXT")
        }
    }
    val V12_TO_V13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS receiver_connection_profiles (id TEXT NOT NULL PRIMARY KEY, transportType TEXT NOT NULL, hostOrAddress TEXT, port INTEGER, bluetoothName TEXT, bluetoothMac TEXT, notes TEXT, savedAtEpochMillis INTEGER NOT NULL)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_receiver_connection_profiles_savedAtEpochMillis ON receiver_connection_profiles(savedAtEpochMillis)")
        }
    }
    /**
     * Favorites are operator-maintained receiver profiles. Existing bench profiles
     * remain intact and are deliberately not promoted to favorites automatically.
     */
    val V13_TO_V14 = object : Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE receiver_connection_profiles ADD COLUMN receiverManufacturer TEXT")
            database.execSQL("ALTER TABLE receiver_connection_profiles ADD COLUMN receiverModel TEXT")
            database.execSQL("ALTER TABLE receiver_connection_profiles ADD COLUMN receiverSerial TEXT")
            database.execSQL("ALTER TABLE receiver_connection_profiles ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_receiver_connection_profiles_isFavorite ON receiver_connection_profiles(isFavorite)")
        }
    }
    val V14_TO_V15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE receiver_connection_profiles ADD COLUMN bluetoothServiceUuid TEXT")
        }
    }

    val V15_TO_V16 = object : Migration(15, 16) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS receiver_catalog (id TEXT NOT NULL PRIMARY KEY, manufacturer TEXT, model TEXT, serialNumber TEXT, firmware TEXT, createdAtEpochMillis INTEGER NOT NULL, archivedAtEpochMillis INTEGER)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_receiver_catalog_createdAtEpochMillis ON receiver_catalog(createdAtEpochMillis)")
            database.execSQL("CREATE TABLE IF NOT EXISTS antenna_catalog (id TEXT NOT NULL PRIMARY KEY, manufacturer TEXT, model TEXT, serialNumber TEXT, createdAtEpochMillis INTEGER NOT NULL, archivedAtEpochMillis INTEGER)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_antenna_catalog_createdAtEpochMillis ON antenna_catalog(createdAtEpochMillis)")
        }
    }

    val V16_TO_V17 = object : Migration(16, 17) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE occupations ADD COLUMN receiverFirmware TEXT")
        }
    }

    val V17_TO_V18 = object : Migration(17, 18) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS audit_events (id TEXT NOT NULL PRIMARY KEY, atEpochMillis INTEGER NOT NULL, action TEXT NOT NULL, actor TEXT NOT NULL, entityId TEXT)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_entityId ON audit_events(entityId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_atEpochMillis ON audit_events(atEpochMillis)")
        }
    }

    /**
     * Preserve the station/reference/equipment values used by each
     * occupation.  Legacy rows are backfilled from the records available at
     * migration time; new rows are written by the repository before later
     * catalogue edits can affect them.
     */
    val V18_TO_V19 = object : Migration(18, 19) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS occupation_snapshots (occupationId TEXT NOT NULL PRIMARY KEY, stationId TEXT, stationName TEXT, stationLocality TEXT, stationMunicipality TEXT, referencePointId TEXT, referenceType TEXT, referenceCode TEXT, referenceDescription TEXT, referenceObservation TEXT, receiverId TEXT, receiverManufacturer TEXT, receiverModel TEXT, receiverSerial TEXT, receiverFirmware TEXT, receiverSource TEXT, antennaId TEXT, antennaManufacturer TEXT, antennaModel TEXT, antennaSerial TEXT, antennaSource TEXT)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_occupation_snapshots_stationId ON occupation_snapshots(stationId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_occupation_snapshots_referencePointId ON occupation_snapshots(referencePointId)")
            database.execSQL("INSERT OR IGNORE INTO occupation_snapshots (occupationId, stationId, stationName, stationLocality, stationMunicipality, referencePointId, referenceType, referenceCode, referenceDescription, referenceObservation, receiverId, receiverManufacturer, receiverModel, receiverSerial, receiverFirmware, receiverSource, antennaId, antennaManufacturer, antennaModel, antennaSerial, antennaSource) SELECT o.id, s.id, s.name, s.locality, s.municipality, r.id, r.type, r.code, r.description, r.observation, NULL, o.receiverManufacturer, o.receiverModel, o.receiverSerial, o.receiverFirmware, CASE WHEN o.receiverManufacturer IS NOT NULL OR o.receiverModel IS NOT NULL OR o.receiverSerial IS NOT NULL OR o.receiverFirmware IS NOT NULL THEN 'OPERATOR' ELSE NULL END, NULL, o.antennaManufacturer, o.antennaModel, o.antennaSerial, CASE WHEN o.antennaManufacturer IS NOT NULL OR o.antennaModel IS NOT NULL OR o.antennaSerial IS NOT NULL THEN 'OPERATOR' ELSE NULL END FROM occupations o LEFT JOIN stations s ON s.id = o.stationId LEFT JOIN reference_points r ON r.id = o.referencePointId WHERE s.id IS NOT NULL OR r.id IS NOT NULL OR o.receiverManufacturer IS NOT NULL OR o.receiverModel IS NOT NULL OR o.receiverSerial IS NOT NULL OR o.receiverFirmware IS NOT NULL OR o.antennaManufacturer IS NOT NULL OR o.antennaModel IS NOT NULL OR o.antennaSerial IS NOT NULL")
        }
    }

    /** Allow an incomplete DRAFT to be persisted before project/station selection. */
    val V19_TO_V20 = object : Migration(19, 20) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE occupations_new (id TEXT NOT NULL PRIMARY KEY, projectId TEXT, stationId TEXT, referencePointId TEXT, plannedDurationSeconds INTEGER, state TEXT NOT NULL, plannedStartEpochMillis INTEGER, confirmedStartEpochMillis INTEGER, confirmedStopEpochMillis INTEGER, receiverModel TEXT, antennaModel TEXT, receiverManufacturer TEXT, antennaManufacturer TEXT, receiverSerial TEXT, antennaSerial TEXT, receiverFirmware TEXT, hasBeforeHeight INTEGER NOT NULL, beforeHeightMeters REAL)")
            database.execSQL("INSERT INTO occupations_new (id, projectId, stationId, referencePointId, plannedDurationSeconds, state, plannedStartEpochMillis, confirmedStartEpochMillis, confirmedStopEpochMillis, receiverModel, antennaModel, receiverManufacturer, antennaManufacturer, receiverSerial, antennaSerial, receiverFirmware, hasBeforeHeight, beforeHeightMeters) SELECT id, projectId, stationId, referencePointId, plannedDurationSeconds, state, plannedStartEpochMillis, confirmedStartEpochMillis, confirmedStopEpochMillis, receiverModel, antennaModel, receiverManufacturer, antennaManufacturer, receiverSerial, antennaSerial, receiverFirmware, hasBeforeHeight, beforeHeightMeters FROM occupations")
            database.execSQL("DROP TABLE occupations")
            database.execSQL("ALTER TABLE occupations_new RENAME TO occupations")
        }
    }

    val ALL = arrayOf(
        V1_TO_V2,
        V2_TO_V3,
        V3_TO_V4,
        V4_TO_V5,
        V5_TO_V6,
        V6_TO_V7,
        V7_TO_V8,
        V8_TO_V9,
        V9_TO_V10,
        V10_TO_V11,
        V11_TO_V12,
        V12_TO_V13,
        V13_TO_V14,
        V14_TO_V15,
        V15_TO_V16,
        V16_TO_V17,
        V17_TO_V18,
        V18_TO_V19,
        V19_TO_V20,
    )
}
