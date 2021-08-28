package org.wikipedia.dataclient.mwapi

import androidx.collection.ArraySet
import org.wikipedia.dataclient.mwapi.MwServiceError.BlockInfo
import org.wikipedia.util.DateUtil
import java.util.*

class UserInfo : BlockInfo() {
    val id = 0
    private val groups: List<String>? = null
    private val latestcontrib: String? = null
    val editCount = 0
    val name: String = ""

    fun getGroups(): Set<String?>? {
        return if (groups != null) ArraySet(groups) else Collections.emptySet()
    }

    val latestContrib: Date
        get() {
            var date = Date(0)
            if (!latestcontrib.isNullOrEmpty()) {
                date = DateUtil.iso8601DateParse(latestcontrib)
            }
            return date
        }
}
