package org.wikipedia.dataclient.watch

import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwResponse

@JsonClass(generateAdapter = true)
data class Watch(val title: String?, val ns: Int, val pageid: Int, val expiry: String?,
                 val watched: Boolean, val unwatched: Boolean, val missing: Boolean) : MwResponse()
