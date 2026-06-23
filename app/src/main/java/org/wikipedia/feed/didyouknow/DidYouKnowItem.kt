package org.wikipedia.feed.didyouknow

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
class DidYouKnowItem(
    val html: String = "",
    val text: String = ""
) : Parcelable
