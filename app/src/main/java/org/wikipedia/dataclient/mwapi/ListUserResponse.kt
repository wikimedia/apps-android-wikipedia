package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName

@Suppress("unused")
class ListUserResponse {

    @SerializedName("cancreate") val canCreate = false
    @SerializedName("cancreateerror") private val canCreateError: List<MwServiceError>? = null
    @SerializedName("name") val name: String = ""
    private val userid: Long = 0
    private val groups: List<String>? = null
    private val missing = false
    val isBlocked get() = error.contains("block")
    val error get() = canCreateError?.get(0)?.title.orEmpty()
}
