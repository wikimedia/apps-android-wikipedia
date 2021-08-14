package org.wikipedia.dataclient.page

import kotlinx.serialization.Serializable
import org.apache.commons.lang3.StringUtils
import java.util.*

/** Protection settings for a page  */
@Serializable
class Protection {

    private val edit: String? = null
    val type: String = ""
    val level: String = ""
    val expiry: String = ""
    val firstAllowedEditorRole: String
        get() = StringUtils.defaultString(edit)
    val editRoles: Set<String>
        get() {
            val roleSet: MutableSet<String> = HashSet()
            if (edit != null) {
                roleSet.add(edit)
            }
            return Collections.unmodifiableSet(roleSet)
        }
}
