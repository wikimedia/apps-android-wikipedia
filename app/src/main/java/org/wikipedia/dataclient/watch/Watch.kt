package org.wikipedia.dataclient.watch

import org.wikipedia.dataclient.mwapi.MwResponse

class Watch : MwResponse() {
    private val title: String? = null
    private val ns = 0
    private val pageid = 0
    private val expiry: String? = null
    private val watched: String? = null
    private val unwatched: String? = null
    private val missing: String? = null

    val isWatched: Boolean
        get() = watched != null

    val isUnWatched: Boolean
        get() = unwatched != null
}