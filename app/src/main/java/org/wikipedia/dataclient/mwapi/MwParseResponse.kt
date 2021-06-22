package org.wikipedia.dataclient.mwapi

class MwParseResponse : MwResponse() {

    private val parse: Parse? = null
    val text: String
        get() = parse?.text.orEmpty()

    private class Parse {
        val text: String? = null
    }
}
