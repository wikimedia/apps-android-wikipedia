package org.wikipedia.dataclient.page

import com.squareup.moshi.JsonClass

/** Protection settings for a page  */
@JsonClass(generateAdapter = true)
class Protection(val edit: String = "", val type: String = "", val level: String = "", val expiry: String = "") {
    val editRoles: Set<String>
        get() = setOf(edit)
}
