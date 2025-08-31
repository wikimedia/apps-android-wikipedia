package org.wikimedia.metricsplatform.context

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Mediawiki context data fields.
 *
 * All fields are nullable, and boxed types are used in place of their equivalent primitive types to avoid
 * unexpected default values from being used where the true value is null.
 */
@Serializable
class MediawikiData(
    @SerialName("database") var database: String? = null
)
