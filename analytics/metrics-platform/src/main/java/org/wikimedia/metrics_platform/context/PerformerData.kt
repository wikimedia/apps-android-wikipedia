@file:UseSerializers(InstantSerializer::class)

package org.wikimedia.metrics_platform.context

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
class PerformerData (
    private val id: Int? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("is_logged_in") val isLoggedIn: Boolean? = null,
    @SerialName("is_temp") val isTemp: Boolean? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("pageview_id") val pageviewId: String? = null,
    @SerialName("groups") val groups: MutableCollection<String?>? = null,
    @SerialName("language_groups") val languageGroups: String? = null,
    @SerialName("language_primary") val languagePrimary: String? = null,
    @SerialName("registration_dt") val registrationDt: Instant? = null
) {
    class PerformerDataBuilder {
        fun languageGroups(languageGroups: String?): PerformerDataBuilder {
            // Ensure that a performer's language groups do not exceed 255 characters. See T361265.

            var languageGroups = languageGroups
            if (languageGroups != null && languageGroups.length > 255) {
                languageGroups = languageGroups.substring(0, 255)
            }

            return this
        }
    }
}
