package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable

@Serializable
class ListUserResponse {

    val name: String = ""
    private val userid: Long = 0
    private val groups: List<String>? = null
    private val missing = false
    private val cancreateerror: List<MwServiceError>? = null
    val cancreate = false
    val isBlocked get() = error.contains("block")
    val error get() = cancreateerror?.get(0)?.title.orEmpty()
}
