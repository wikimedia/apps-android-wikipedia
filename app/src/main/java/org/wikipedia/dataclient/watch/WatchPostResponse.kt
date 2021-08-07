package org.wikipedia.dataclient.watch

import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwPostResponse

@JsonClass(generateAdapter = true)
class WatchPostResponse(internal val batchcomplete: String? = null, val watch: List<Watch> = emptyList()) : MwPostResponse() {
    val first: Watch?
        get() = watch.firstOrNull()
}
