package org.wikipedia.dataclient.restbase

import org.wikipedia.dataclient.ServiceError
import org.wikipedia.json.GsonUnmarshaller

class RbServiceError : ServiceError {

    private val type: String? = null
    override val title: String = ""
    override val details: String = ""
    private val detail: String? = null
    private val method: String? = null
    private val uri: String? = null

    companion object {
        fun create(rspBody: String): RbServiceError {
            return GsonUnmarshaller.unmarshal(RbServiceError::class.java, rspBody)
        }
    }
}
