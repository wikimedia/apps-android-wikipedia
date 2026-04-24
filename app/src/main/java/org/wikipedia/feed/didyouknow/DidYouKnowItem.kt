package org.wikipedia.feed.didyouknow

import kotlinx.serialization.Serializable

@Serializable
class DidYouKnowItem(
    val html: String = "",
    val text: String = ""
)
