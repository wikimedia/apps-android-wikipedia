package org.wikipedia.feed.topread

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.parcel.DateParceler
import java.util.*

@Parcelize
@TypeParceler<Date, DateParceler>()
class TopRead(val date: Date = Date(), val articles: List<PageSummary> = emptyList()) : Parcelable
