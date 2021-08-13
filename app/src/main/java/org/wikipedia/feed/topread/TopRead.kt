package org.wikipedia.feed.topread

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary
import java.util.*

@Parcelize
@Serializable
class TopRead(val date: @Contextual Date = Date(), val articles: List<PageSummary> = emptyList()) :
    Parcelable
