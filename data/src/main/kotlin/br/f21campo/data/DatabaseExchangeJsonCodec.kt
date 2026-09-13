package br.f21campo.data

import org.json.JSONArray
import org.json.JSONObject

/** Android JSON codec for the versioned exchange envelope. */
object DatabaseExchangeJsonCodec {
    fun decodeCore(jsonText: String): DatabaseExchangeEnvelope {
        val root = JSONObject(jsonText)
        val envelope = DatabaseExchangeEnvelope(
            format = root.optString("format"),
            formatVersion = root.optInt("formatVersion", -1),
            exportedAtEpochMillis = root.optLong("exportedAtEpochMillis", 0L),
            projects = root.optJSONArray("projects").toObjects { item -> ProjectEntity(item.getString("id"), item.getString("name"), item.getLong("createdAtEpochMillis"), item.optNullableLong("archivedAtEpochMillis")) },
            stations = root.optJSONArray("stations").toObjects { item -> StationEntity(item.getString("id"), item.getString("name"), item.optNullableString("locality"), item.optNullableString("municipality"), item.getLong("createdAtEpochMillis"), item.optNullableLong("archivedAtEpochMillis")) },
            referencePoints = root.optJSONArray("referencePoints").toObjects { item -> ReferencePointEntity(item.getString("id"), item.getString("stationId"), item.getString("type"), item.getString("code"), item.optNullableString("description"), item.optNullableString("observation")) },
        )
        envelope.validate().getOrThrow()
        return envelope
    }

    fun encode(envelope: DatabaseExchangeEnvelope): String {
        envelope.validate().getOrThrow()
        return JSONObject().apply {
            put("format", envelope.format)
            put("formatVersion", envelope.formatVersion)
            put("exportedAtEpochMillis", envelope.exportedAtEpochMillis)
            put("projects", JSONArray(envelope.projects.map { JSONObject().put("id", it.id).put("name", it.name).put("createdAtEpochMillis", it.createdAtEpochMillis).putNullable("archivedAtEpochMillis", it.archivedAtEpochMillis) }))
            put("stations", JSONArray(envelope.stations.map { JSONObject().put("id", it.id).put("name", it.name).putNullable("locality", it.locality).putNullable("municipality", it.municipality).put("createdAtEpochMillis", it.createdAtEpochMillis).putNullable("archivedAtEpochMillis", it.archivedAtEpochMillis) }))
            put("referencePoints", JSONArray(envelope.referencePoints.map { JSONObject().put("id", it.id).put("stationId", it.stationId).put("type", it.type).put("code", it.code).putNullable("description", it.description).putNullable("observation", it.observation) }))
            put("occupations", JSONArray(envelope.occupations.map { JSONObject().put("id", it.id).put("projectId", it.projectId).put("stationId", it.stationId).putNullable("referencePointId", it.referencePointId).putNullable("plannedDurationSeconds", it.plannedDurationSeconds).put("state", it.state).putNullable("plannedStartEpochMillis", it.plannedStartEpochMillis).putNullable("confirmedStartEpochMillis", it.confirmedStartEpochMillis).putNullable("confirmedStopEpochMillis", it.confirmedStopEpochMillis).putNullable("receiverModel", it.receiverModel).putNullable("antennaModel", it.antennaModel).put("hasBeforeHeight", it.hasBeforeHeight).putNullable("beforeHeightMeters", it.beforeHeightMeters) }))
            put("heightMeasurements", JSONArray(envelope.heightMeasurements.map { JSONObject().put("id", it.id).put("occupationId", it.occupationId).put("phase", it.phase).put("valueMeters", it.valueMeters).put("type", it.type).putNullable("observedAtEpochMillis", it.observedAtEpochMillis).putNullable("observation", it.observation) }))
            put("events", JSONArray(envelope.events.map { JSONObject().put("id", it.id).put("occupationId", it.occupationId).put("atEpochMillis", it.atEpochMillis).put("category", it.category).put("severity", it.severity).put("description", it.description).put("source", it.source) }))
            put("artifacts", JSONArray(envelope.artifacts.map { JSONObject().put("id", it.id).put("occupationId", it.occupationId).put("role", it.role).put("path", it.path).put("sizeBytes", it.sizeBytes).put("sha256", it.sha256).put("importedAtEpochMillis", it.importedAtEpochMillis) }))
            put("receiverConnectionProfiles", JSONArray(envelope.receiverConnectionProfiles.map { JSONObject().put("id", it.id).putNullable("receiverManufacturer", it.receiverManufacturer).putNullable("receiverModel", it.receiverModel).putNullable("receiverSerial", it.receiverSerial).put("isFavorite", it.isFavorite).put("transportType", it.transportType).putNullable("hostOrAddress", it.hostOrAddress).putNullable("port", it.port).putNullable("bluetoothName", it.bluetoothName).putNullable("bluetoothMac", it.bluetoothMac).putNullable("bluetoothServiceUuid", it.bluetoothServiceUuid).putNullable("notes", it.notes).put("savedAtEpochMillis", it.savedAtEpochMillis) }))
        }.toString(2)
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)

    private inline fun <T> JSONArray?.toObjects(transform: (JSONObject) -> T): List<T> = buildList {
        if (this@toObjects == null) return@buildList
        for (index in 0 until length()) add(transform(getJSONObject(index)))
    }

    private fun JSONObject.optNullableString(key: String): String? = if (isNull(key)) null else optString(key).ifBlank { null }
    private fun JSONObject.optNullableLong(key: String): Long? = if (isNull(key)) null else optLong(key)
}
