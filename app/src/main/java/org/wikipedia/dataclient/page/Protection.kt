package org.wikipedia.dataclient.page

import kotlinx.serialization.Serializable

/** Protection settings for a page  */
@Serializable
class Protection {

    private val edit: String? = null
    val type: String = ""
    val level: String = ""
    val expiry: String = ""
    val firstAllowedEditorRole: String
        get() = edit.orEmpty()
    val editRoles: Set<String>
        get() {
            val roleSet = mutableSetOf<String>()
            edit?.let { roleSet.add(edit) }
            return roleSet.toSet()
        }
}
