package org.wikipedia.dataclient.restbase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.json.MoshiUtil
import java.io.IOException

/**
 * Moshi POJO for a RESTBase API error.
 */
@JsonClass(generateAdapter = true)
class RbServiceError(
    internal val type: String = "",
    override val title: String = "",
    @Json(name = "detail") override val details: String = "",
    internal val method: String = "",
    internal val uri: String = ""
) : ServiceError {
    companion object {
        @Throws(IOException::class)
        fun create(rspBody: String): RbServiceError? {
            return MoshiUtil.getDefaultMoshi().adapter(RbServiceError::class.java).fromJson(rspBody)
        }
    }
}
