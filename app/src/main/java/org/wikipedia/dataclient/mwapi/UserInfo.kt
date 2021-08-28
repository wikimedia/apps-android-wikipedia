package org.wikipedia.dataclient.mwapi

import androidx.collection.ArraySet
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwServiceError.BlockInfo
import org.wikipedia.util.DateUtil
import java.util.*

@SuppressWarnings("unused")
@Serializable
class UserInfo : BlockInfo() {
    val id = 0
    private val groups: List<String>? = null
    private val latestcontrib: String? = null
    val editcount = 0
    val name: String = ""

    fun groups(): Set<String> {
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
