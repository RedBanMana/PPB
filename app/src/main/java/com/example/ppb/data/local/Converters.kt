package com.example.ppb.data.local

import androidx.room.TypeConverter
import kotlinx.datetime.Instant

class Converters {
    @TypeConverter
    fun instantToEpochLong(value: Long): Instant {
        return Instant.fromEpochSeconds(value)
    }

    @TypeConverter
    fun epochLongToInstant(instant: Instant): Long {
        return instant.toEpochMilliseconds() / 1000
    }
}
