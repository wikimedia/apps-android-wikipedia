package org.wikipedia.dataclient.watch

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwPostResponse

@Serializable
class WatchPostResponse : MwPostResponse() {
    val batchcomplete: String? = null

    val watch: List<Watch>? = null
        get() = field ?: emptyList()

    fun getFirst(): Watch? {
        return watch?.get(0)
    }
}
