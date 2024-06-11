package org.wikipedia.dataclient.restbase

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.json.JsonUtil

/**
 * Model class that can represent either a RESTBase error or a MediaWiki REST API error.
 * Since both types of errors have non-overlapping fields, we can use a single class to
 * represent either of them, and then phase out the RESTBase-specific fields when RESTBase
 * is fully decommissioned.
 */
@Serializable
class RbServiceError : ServiceError {

    // These fields are given by RESTBase errors, and should be removed when RESTBase
    // is fully decommissioned.
    private val title: String? = null
    private val detail: String? = null

    // These fields are given by MediaWiki REST API errors, and should be preferred.
    private val errorKey: String? = null
    private val messageTranslations: Map<String, String>? = null

    override val key get() = title ?: errorKey.orEmpty()

    override val message: String get() {
        return if (messageTranslations != null) {
            messageTranslations.values.firstOrNull() ?: ""
        } else {
            detail.orEmpty()
        }
    }

    companion object {
        fun create(rspBody: String): RbServiceError {
            return JsonUtil.decodeFromString(rspBody)!!
        }
    }
}
