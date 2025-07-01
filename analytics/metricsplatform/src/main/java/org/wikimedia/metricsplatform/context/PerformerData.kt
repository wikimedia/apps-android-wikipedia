@file:UseSerializers(InstantSerializer::class)

package org.wikimedia.metricsplatform.context

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

/**
 * Performer context data fields.
 *
 * All fields are nullable, and boxed types are used in place of their equivalent primitive types to avoid
 * unexpected default values from being used where the true value is null.
 */
@Serializable
data class PerformerData (
    var id: Int? = null,
    @SerialName("name") var name: String? = null,
    @SerialName("is_logged_in") var isLoggedIn: Boolean? = null,
    @SerialName("is_temp") var isTemp: Boolean? = null,
    @SerialName("session_id") var sessionId: String? = null,
    @SerialName("pageview_id") var pageviewId: String? = null,
    @SerialName("groups") var groups: Collection<String>? = null,
    @SerialName("language_groups") var languageGroups: String? = null,
    @SerialName("language_primary") var languagePrimary: String? = null,
    @SerialName("registration_dt") var registrationDt: Instant? = null
)
