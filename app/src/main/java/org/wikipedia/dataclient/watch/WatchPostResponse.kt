package org.wikipedia.dataclient.watch

import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwPostResponse
import org.wikipedia.dataclient.mwapi.MwServiceError

@JsonClass(generateAdapter = true)
class WatchPostResponse(
    errors: List<MwServiceError> = emptyList(),
    internal val batchcomplete: String? = null,
    val watch: List<Watch> = emptyList()
) : MwPostResponse(errors) {
    val first: Watch?
        get() = watch.firstOrNull()
}
