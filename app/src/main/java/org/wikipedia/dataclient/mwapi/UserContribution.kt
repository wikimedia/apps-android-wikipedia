package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
@OptIn(ExperimentalTime::class)
class UserContribution(
    val userid: Int = 0,
    val user: String = "",
    val pageid: Int = 0,
    val revid: Long = 0,
    val parentid: Long = 0,
    val ns: Int = 0,
    val title: String = "",
    val timestamp: Instant,
    val comment: String = "",
    val new: Boolean = false,
    val minor: Boolean = false,
    val top: Boolean = false,
    val size: Int = 0,
    val sizediff: Int = 0,
    val tags: List<String> = emptyList(),
)
