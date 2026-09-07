package br.f21campo.data

import androidx.room.TypeConverter

class F21Converters {
    @TypeConverter fun instantToEpochMillis(value: Long?): java.time.Instant? = value?.let(java.time.Instant::ofEpochMilli)
    @TypeConverter fun instantToEpochMillis(value: java.time.Instant?): Long? = value?.toEpochMilli()
}
