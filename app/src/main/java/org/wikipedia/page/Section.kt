package org.wikipedia.page

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Section(var id: Int = 0,
                   var level: Int = 1,
                   @SerialName("title") private var _title: String?,
                   @SerialName("anchor") private var _anchor: String?,
                   @SerialName("text") private var _text: String?) {
    val isLead get() = id == 0
    val heading get() = _title.orEmpty()
    val anchor get() = _anchor.orEmpty()
    val content get() = _text.orEmpty()
}
