package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class MwParseResponse(internal val parse: Parse? = null) : MwResponse() {
    val text: String
        get() = parse?.text.orEmpty()

    @JsonClass(generateAdapter = true)
    class Parse(val text: String = "")
}
