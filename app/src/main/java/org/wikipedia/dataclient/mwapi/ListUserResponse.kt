package org.wikipedia.dataclient.mwapi

import androidx.collection.ArraySet
import com.google.gson.annotations.SerializedName

class ListUserResponse {

    @SerializedName("name") val name: String = ""
    private val userid: Long = 0
    private val groups: List<String>? = null
    private val missing = false
    val cancreate = false
    private val cancreateerror: List<MwServiceError>? = null
    val isBlocked: Boolean
        get() = error.contains("block")
    val error: String
        get() = if (cancreateerror != null && cancreateerror.isNotEmpty()) cancreateerror[0].title else ""

    fun getGroups(): Set<String> {
        return if (groups != null) ArraySet(groups) else emptySet()
    }
}
