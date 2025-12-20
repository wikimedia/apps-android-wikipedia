package org.wikimedia.testkitchen.context

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mediawiki context data fields.
 */
@Serializable
class MediawikiData(
    @SerialName("database") var database: String? = null
)
