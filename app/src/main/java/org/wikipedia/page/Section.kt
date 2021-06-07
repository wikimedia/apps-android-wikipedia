package org.wikipedia.page

import com.google.gson.annotations.SerializedName
import org.wikipedia.json.GsonUtil
import java.util.*

data class Section(var id: Int = 0,
                   var level: Int = 1,
                   @SerializedName("title") private var _title: String?,
                   @SerializedName("anchor") private var _anchor: String?,
                   @SerializedName("text") private var _text: String?) {

    val isLead get() = id == 0
    val heading get() = _title.orEmpty()
    val anchor get() = _anchor.orEmpty()
    val content get() = _text.orEmpty()

    override fun equals(other: Any?): Boolean {
        if (other !is Section) {
            return false
        }
        return id == other.id && level == other.level && heading == other.heading &&
                anchor == other.anchor && content == other.content
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + level.hashCode()
        result = 31 * result + heading.hashCode()
        result = 31 * result + anchor.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }

    override fun toString(): String {
        return "Section{id=$id, level=$level, heading='$heading', anchor='$anchor', content='$content'}"
    }

    companion object {
        fun fromJson(json: String?): List<Section> {
            return GsonUtil.getDefaultGson().fromJson(json, Array<Section>::class.java).toList()
        }
    }
}
