package org.wikipedia.feed.topread

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wikipedia.dataclient.page.PageSummary
import java.util.*

@Parcelize
class TopRead(val date: Date = Date(), val articles: List<PageSummary> = emptyList()) :
    Parcelable
