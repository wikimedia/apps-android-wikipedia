package org.wikimedia.metrics_platform.context

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
    val id: Int? = null,
    val title: String? = null,
    @SerialName("namespace_id") val namespaceId: Int? = null,
    @SerialName("namespace_name") val namespaceName: String? = null,
    @SerialName("revision_id") val revisionId: Long? = null,
    @SerialName("wikidata_qid") val wikidataItemQid: String? = null,
    @SerialName("content_language") val contentLanguage: String? = null,
)
