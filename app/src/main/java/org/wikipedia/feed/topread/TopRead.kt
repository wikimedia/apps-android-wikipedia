package org.wikipedia.feed.topread

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.parcel.DateParceler
import org.wikipedia.serialization.DateSerializer
import java.util.*

@Serializable
@Parcelize
@TypeParceler<Date, DateParceler>()
class TopRead(@Serializable(with = DateSerializer::class) val date: Date = Date(), val articles: List<PageSummary> = emptyList()) : Parcelable
