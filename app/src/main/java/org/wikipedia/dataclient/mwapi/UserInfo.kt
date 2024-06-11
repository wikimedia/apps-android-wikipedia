package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwServiceError.BlockInfo
import org.wikipedia.util.DateUtil
import java.util.*

@Serializable
class UserInfo : BlockInfo() {
    val id = 0
    private val groups: List<String>? = null
    @SerialName("latestcontrib") private val latestContrib: String? = null
    @SerialName("registrationdate") private val regDate: String? = null
    @SerialName("registration") private val registration: String? = null
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

    val latestContribDate: Date
        get() {
            var date = Date(0)
            if (!latestContrib.isNullOrEmpty()) {
                date = DateUtil.iso8601DateParse(latestContrib)
            }
            return date
        }

    val registrationDate: Date
        get() {
            var date = Date(0)
            if (!regDate.isNullOrEmpty()) {
                date = DateUtil.iso8601DateParse(regDate)
            } else if (!registration.isNullOrEmpty()) {
                date = DateUtil.iso8601DateParse(registration)
            }
            return date
        }

    @Serializable
    class Options {
        @SerialName("watchdefault") val watchDefault: Int = 0
    }
}
