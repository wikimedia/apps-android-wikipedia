package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwServiceError.BlockInfo
import org.wikipedia.json.LocalDateAsTimestamp
import org.wikipedia.util.DateUtil
import java.util.*

@Serializable
class UserInfo : BlockInfo() {
    val id = 0
    private val groups: List<String>? = null
    @SerialName("latestcontrib") val latestContrib: LocalDateAsTimestamp = DateUtil.EPOCH_DATE
    @SerialName("registrationdate") private val regDate: LocalDateAsTimestamp? = null
    @SerialName("registration") private val registration: LocalDateAsTimestamp? = null
    @SerialName("editcount") val editCount = -1
    val name: String = ""
    val anon: Boolean = false
    val messages: Boolean = false
    val rights: List<String> = emptyList()
    @SerialName("cancreate") val canCreate: Boolean = false
    @SerialName("cancreateerror") private val canCreateError: List<MwServiceError>? = null
    val options: Options? = null

    val error get() = canCreateError?.get(0)?.title.orEmpty()
    val hasBlockError get() = error.contains("block")

    fun groups(): Set<String> {
        return groups?.toSet() ?: emptySet()
    }

    val registrationDate
        get() = regDate ?: registration ?: DateUtil.EPOCH_DATE

    @Serializable
    class Options {
        @SerialName("watchdefault") val watchDefault: Int = 0
    }
}
