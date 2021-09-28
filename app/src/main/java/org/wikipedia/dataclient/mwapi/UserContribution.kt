package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.util.DateUtil
import java.text.ParseException
import java.util.*

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
    @Transient private var parsedDate: Date? = null
    val comment: String = ""
    val new: Boolean = false
    val minor: Boolean = false
    val top: Boolean = false
    val size: Int = 0
    val sizediff: Int = 0
    val tags: List<String> = Collections.emptyList()

    fun date(): Date {
        if (parsedDate == null) {
            try {
                parsedDate = DateUtil.iso8601DateParse(timestamp)
            } catch (e: ParseException) {
                // ignore
            }
        }
        return parsedDate!!
    }
}
