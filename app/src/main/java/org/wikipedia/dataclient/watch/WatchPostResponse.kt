package org.wikipedia.dataclient.watch

import org.wikipedia.dataclient.mwapi.MwPostResponse

class WatchPostResponse : MwPostResponse() {
    private val batchcomplete: String? = null

    val watch: List<Watch>? = null
        get() = field ?: emptyList()

    fun getFirst(): Watch? {
        return watch?.get(0)
    }
}
