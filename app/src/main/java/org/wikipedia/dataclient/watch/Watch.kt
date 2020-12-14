package org.wikipedia.dataclient.watch

import org.wikipedia.dataclient.mwapi.MwResponse

class Watch : MwResponse() {
    private val title: String? = null
    private val ns = 0
    private val pageid = 0
    private val expiry: String? = null
    private val watched: Boolean = false
    private val unwatched: Boolean = false
    private val missing: Boolean = false

    val isWatched: Boolean
        get() = watched

    val isUnWatched: Boolean
        get() = unwatched
}