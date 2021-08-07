package org.wikipedia.page

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Section(var id: Int = 0, var level: Int = 1, var title: String = "", var anchor: String = "",
                   var text: String = "") {
    val isLead get() = id == 0
}
