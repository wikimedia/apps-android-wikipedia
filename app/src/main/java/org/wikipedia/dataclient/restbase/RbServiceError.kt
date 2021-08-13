package org.wikipedia.dataclient.restbase

import org.apache.commons.lang3.StringUtils
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.json.GsonUnmarshaller

class RbServiceError : ServiceError {

    private val type: String? = null
    private val title: String = ""
    private val detail: String? = null
    private val method: String? = null
    private val uri: String? = null

    override fun getTitle(): String {
        return StringUtils.defaultString(title)
    }

    override fun getDetails(): String {
        return StringUtils.defaultString(detail)
    }

    companion object {
        fun create(rspBody: String): RbServiceError {
            return GsonUnmarshaller.unmarshal(RbServiceError::class.java, rspBody)
        }
    }
}
