package org.wikipedia.dataclient.watch

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwPostResponse
import org.wikipedia.dataclient.mwapi.MwServiceError

@JsonClass(generateAdapter = true)
class WatchPostResponse(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") servedBy: String = "",
    internal val batchcomplete: String = "",
    val watch: List<Watch> = emptyList()
) : MwPostResponse(errors, servedBy) {
    val first: Watch?
        get() = watch.firstOrNull()
}
