package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ShortenUrlResponse : MwResponse() {
    @SerialName("shortenurl") val shortenUrl: ShortenUrl? = null

    @Serializable
    class ShortenUrl {
        @SerialName("shorturl") val shortUrl: String? = null
        @SerialName("shorturlalt") val shortUrlAlt: String? = null
    }
}
