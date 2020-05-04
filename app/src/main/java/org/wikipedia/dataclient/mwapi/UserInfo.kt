package org.wikipedia.dataclient.mwapi

import androidx.collection.ArraySet
import org.wikipedia.util.DateUtil
import java.text.ParseException
import java.util.*

@Suppress("UNUSED")
class UserInfo {
    private val name: String? = null
    val id = 0
    private val groups: List<String>? = null
    private val blockid = 0
    private val blockreason: String? = null
    private val blockedby: String? = null
    private val blockedtimestamp: String? = null
    private val blockexpiry: String? = null

    val groupsAsSet: Set<String>
        get() = groups?.let { ArraySet(it) } ?: emptySet()

    // ignore
    val isBlocked: Boolean
        get() {
            if (blockexpiry.isNullOrEmpty()) {
                return false
            }
            try {
                val now = Date()
                val expiry = DateUtil.iso8601DateParse(blockexpiry)
                if (expiry.after(now)) {
                    return true
                }
            } catch (e: ParseException) {
                // ignore
            }
            return false
        }
}
