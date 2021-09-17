package org.wikipedia.feed.topread

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.DateUtil
import java.lang.Exception
import java.util.*

@Serializable
@Parcelize
class TopRead(
    val date: String = "",
    val articles: List<PageSummary> = emptyList()
) : Parcelable {
    fun date(): Date {
        try {
            return DateUtil.iso8601ShortDateParse(date)
        } catch (e: Exception) {}
        return Date()
    }
}
