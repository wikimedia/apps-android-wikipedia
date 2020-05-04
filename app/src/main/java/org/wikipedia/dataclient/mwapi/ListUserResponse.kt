package org.wikipedia.dataclient.mwapi

import androidx.collection.ArraySet

@Suppress("UNUSED")
class ListUserResponse {
    val name: String? = null
        get() = field ?: ""

    private val userid: Long = 0
    private val groups: List<String>? = null
    private val missing = false

    @get:JvmName("canCreate")
    val cancreate = false

    private val cancreateerror: List<MwServiceError>? = null

    val isBlocked: Boolean
        get() = error.contains("block")

    val error: String
        get() = if (!cancreateerror.isNullOrEmpty()) cancreateerror[0].title else ""

    val groupsAsSet: Set<String>
        get() = groups?.let { ArraySet(it) } ?: emptySet()
}
