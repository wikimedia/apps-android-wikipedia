package org.wikipedia.parcel

import android.os.Parcel
import kotlinx.parcelize.Parceler
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object LocalDateTimeParceler : Parceler<LocalDateTime> {
    override fun create(parcel: Parcel): LocalDateTime {
        return LocalDateTime.parse(parcel.readString())
    }

    override fun LocalDateTime.write(parcel: Parcel, flags: Int) {
        parcel.writeString(format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }
}
