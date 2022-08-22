package org.wikipedia.dataclient.mwapi

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwServiceError.BlockInfo

@Serializable
class UserInfo : BlockInfo() {
    val id = 0
    private val groups: List<String>? = null
    @SerialName("latestcontrib") val latestContribInstant: Instant = Instant.DISTANT_PAST
    @SerialName("registrationdate") private val regInstant: Instant? = null
    private val registration: Instant? = null
    @SerialName("editcount") val editCount = -1
    val name: String = ""
    val anon: Boolean = false
    val messages: Boolean = false
    val rights: List<String> = emptyList()
    @SerialName("cancreate") val canCreate: Boolean = false
    @SerialName("cancreateerror") private val canCreateError: List<MwServiceError>? = null

    val error get() = canCreateError?.get(0)?.title.orEmpty()
    val hasBlockError get() = error.contains("block")

    fun groups(): Set<String> {
        return groups?.toSet() ?: emptySet()
    }

    val registrationDate: Instant
        get() = regInstant ?: registration ?: Instant.DISTANT_PAST
}
