package org.wikipedia.dataclient.mwapi

import androidx.collection.ArraySet
import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.mwapi.MwServiceError.BlockInfo
import org.wikipedia.util.DateUtil
import java.util.*

class UserInfo : BlockInfo() {
    val id = 0
    private val groups: List<String>? = null
    @SerializedName("latestcontrib") private val latestContrib: String? = null
    @SerializedName("editcount") val editCount = 0
    val name: String = ""

    fun groups(): Set<String> {
        return if (groups != null) ArraySet(groups) else Collections.emptySet()
    }

    val latestContribDate: Date
        get() {
            var date = Date(0)
            if (!latestContrib.isNullOrEmpty()) {
                date = DateUtil.iso8601DateParse(latestContrib)
            }
            return date
        }
}
