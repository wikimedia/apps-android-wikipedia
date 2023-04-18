package org.wikipedia.feed.topread

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Serializable
@Parcelize
class TopRead(
    val date: String = "",
    val articles: List<PageSummary> = emptyList()
) : Parcelable {
    @IgnoredOnParcel
    val localDate: LocalDate by lazy {
        try {
            LocalDate.parse(date, DateTimeFormatter.ISO_OFFSET_DATE)
        } catch (e: DateTimeParseException) {
            LocalDate.now()
        }
    }
}
