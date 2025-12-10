package org.wikipedia.parcel

import android.os.Parcel
import kotlinx.parcelize.Parceler
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

object LocalDateTimeParceler : Parceler<LocalDateTime> {
    override fun LocalDateTime.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(toInstant(ZoneOffset.UTC).toEpochMilli())
    }

    override fun create(parcel: Parcel): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(parcel.readLong()), ZoneOffset.UTC)
}
