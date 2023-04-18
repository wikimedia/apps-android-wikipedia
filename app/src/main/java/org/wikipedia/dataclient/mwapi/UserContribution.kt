package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable
import org.wikipedia.util.DateUtil

@Serializable
class UserContribution {
    val userid: Int = 0
    val user: String = ""
    val pageid: Int = 0
    val revid: Long = 0
    val parentid: Long = 0
    val ns: Int = 0
    val title: String = ""
    private val timestamp: String = ""
    val comment: String = ""
    val new: Boolean = false
    val minor: Boolean = false
    val top: Boolean = false
    val size: Int = 0
    val sizediff: Int = 0
    val tags: List<String> = emptyList()

    val parsedDateTime by lazy { DateUtil.iso8601LocalDateTimeParse(timestamp) }
}
