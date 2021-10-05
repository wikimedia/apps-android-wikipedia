package org.wikipedia.dataclient.restbase

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.json.JsonUtil

@Serializable
class RbServiceError : ServiceError {

    private val type: String? = null
    private val detail: String? = null
    private val method: String? = null
    private val uri: String? = null

    override val title: String = ""

    override val details: String get() = detail.orEmpty()

    companion object {
        fun create(rspBody: String): RbServiceError {
            return JsonUtil.decodeFromString(rspBody)!!
        }
    }
}
