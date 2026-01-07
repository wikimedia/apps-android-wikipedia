package org.wikimedia.testkitchen.context

import kotlinx.serialization.Serializable

/**
 * Mediawiki context data fields.
 */
@Serializable
class MediawikiData(
    var database: String? = null
)
