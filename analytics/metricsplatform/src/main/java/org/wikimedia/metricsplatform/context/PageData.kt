package org.wikimedia.metricsplatform.context

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Page context data context fields.
 *
 * PageData are dynamic context fields that change with every request. PageData is submitted with each event
 * by the client and then queued for processing by EventProcessor.
 *
 * All fields are nullable, and boxed types are used in place of their equivalent primitive types to avoid
 * unexpected default values from being used where the true value is null.
 */
@Serializable
class PageData(
    var id: Int? = null,
    var title: String? = null,
    @SerialName("namespace_id") var namespaceId: Int? = null,
    @SerialName("namespace_name") var namespaceName: String? = null,
    @SerialName("revision_id") var revisionId: Long? = null,
    @SerialName("wikidata_qid") var wikidataItemQid: String? = null,
    @SerialName("content_language") var contentLanguage: String? = null,
)
