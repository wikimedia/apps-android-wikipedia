package org.wikipedia.feed.topread

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.serialization.DynamicLookupSerializer
import java.util.*

@Parcelize
@Serializable
class TopRead(@Serializable(with = DynamicLookupSerializer::class) val date: Date = Date(),
              val articles: List<PageSummary> = emptyList()) : Parcelable
