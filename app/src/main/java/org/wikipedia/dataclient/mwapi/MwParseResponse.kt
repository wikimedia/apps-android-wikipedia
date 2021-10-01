package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable

@Serializable
class MwParseResponse : MwResponse() {

    private val parse: Parse? = null
    val text: String
        get() = parse?.text.orEmpty()

    @Serializable
    private class Parse {
        val text: String? = null
    }
}
