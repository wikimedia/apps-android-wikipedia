package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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

    val parsedInstant: Instant by lazy {
        Instant.parse(timestamp)
    }
    val parsedDateTime: LocalDateTime by lazy {
        LocalDateTime.ofInstant(parsedInstant, ZoneId.systemDefault())
    }
}
