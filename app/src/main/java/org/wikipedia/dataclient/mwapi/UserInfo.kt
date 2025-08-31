package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.wikipedia.dataclient.mwapi.MwServiceError.BlockInfo
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@Serializable
@OptIn(ExperimentalTime::class)
class UserInfo : BlockInfo() {
    val id = 0
    private val groups: List<String>? = null
    @SerialName("latestcontrib") private val latestContrib: Instant = Instant.DISTANT_PAST
    @SerialName("registrationdate") private val regInstant: Instant? = null
    private val registration: Instant? = null
    @SerialName("editcount") val editCount = -1
    val name: String = ""
    val anon: Boolean = false
    val messages: Boolean = false
    val rights: List<String> = emptyList()
    @SerialName("cancreate") val canCreate: Boolean = false
    @SerialName("cancreateerror") private val canCreateError: List<MwServiceError>? = null
    val options: Options? = null

    val error get() = canCreateError?.get(0)?.key.orEmpty()
    val hasBlockError get() = error.contains("block")

    fun groups(): Set<String> {
        return groups?.toSet() ?: emptySet()
    }

    val latestContribDate: LocalDate by lazy {
        LocalDate.ofInstant(latestContrib.toJavaInstant(), ZoneId.systemDefault())
    }

    val registrationDate: LocalDate by lazy {
        val instant = regInstant ?: registration ?: Instant.DISTANT_PAST
        LocalDate.ofInstant(instant.toJavaInstant(), ZoneId.systemDefault())
    }

    @Serializable
    class Options {
        @SerialName("watchdefault") val watchDefault: Int = 0
        @SerialName("centralnotice-display-campaign-type-fundraising") val fundraisingOptIn: JsonElement? = null
    }
}
