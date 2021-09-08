package org.wikipedia.parcel

import android.os.Parcel
import kotlinx.parcelize.Parceler
import java.util.*

object DateParceler : Parceler<Date> {
    override fun create(parcel: Parcel) = Date(parcel.readLong())

    override fun Date.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(time)
    }
}
