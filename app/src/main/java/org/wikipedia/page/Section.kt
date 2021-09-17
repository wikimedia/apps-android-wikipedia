package org.wikipedia.page

import kotlinx.serialization.Serializable

@Serializable
data class Section(var id: Int = 0,
                   var level: Int = 1,
                   val title: String = "",
                   val anchor: String = "",
                   val text: String = "") {
    val isLead get() = id == 0
}
