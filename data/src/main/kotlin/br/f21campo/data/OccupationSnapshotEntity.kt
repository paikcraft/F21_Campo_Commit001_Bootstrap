package br.f21campo.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One immutable-at-the-application-level snapshot per occupation.  All
 * copied fields are nullable so a migration can preserve legacy occupations
 * whose older versions did not collect every value.
 */
@Entity(
    tableName = "occupation_snapshots",
    indices = [Index("stationId"), Index("referencePointId")],
)
data class OccupationSnapshotEntity(
    @PrimaryKey val occupationId: String,
    val stationId: String?,
    val stationName: String?,
    val stationLocality: String?,
    val stationMunicipality: String?,
    val referencePointId: String?,
    val referenceType: String?,
    val referenceCode: String?,
    val referenceDescription: String?,
    val referenceObservation: String?,
    val receiverId: String?,
    val receiverManufacturer: String?,
    val receiverModel: String?,
    val receiverSerial: String?,
    val receiverFirmware: String?,
    val receiverSource: String?,
    val antennaId: String?,
    val antennaManufacturer: String?,
    val antennaModel: String?,
    val antennaSerial: String?,
    val antennaSource: String?,
)
