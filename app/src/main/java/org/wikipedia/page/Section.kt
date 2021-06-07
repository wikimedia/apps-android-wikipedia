package org.wikipedia.page

import com.google.gson.annotations.SerializedName

data class Section(var id: Int = 0,
                   var level: Int = 1,
                   @SerializedName("title") private var _title: String?,
                   @SerializedName("anchor") private var _anchor: String?,
                   @SerializedName("text") private var _text: String?) {
    val isLead get() = id == 0
    val heading get() = _title.orEmpty()
    val anchor get() = _anchor.orEmpty()
    val content get() = _text.orEmpty()
}
