package org.wikipedia.analytics.eventplatform

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.util.DateUtil.iso8601DateFormat
import java.util.*

/** Base class for an Event Platform event. */
@JsonClass(generateAdapter = true)
open class Event(
    @Json(name = "\$schema") val schema: String = "",
    internal val meta: Meta = Meta(""),
    internal val dt: String = iso8601DateFormat(Date()),
    @Json(name = "app_session_id") var sessionId: String = "",
    @Json(name = "app_install_id") var appInstallId: String? = null
) {
    constructor(schema: String, stream: String) : this(schema, Meta(stream))

    val stream: String
        get() = meta.stream

    @JsonClass(generateAdapter = true)
    class Meta(val stream: String)
}
