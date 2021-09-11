package org.wikipedia.feed.topread

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.parcel.DateParceler
import java.util.*

@JsonClass(generateAdapter = true)
@Parcelize
@TypeParceler<Date, DateParceler>()
class TopRead(val date: Date = Date(), val articles: List<PageSummary> = emptyList()) : Parcelable
