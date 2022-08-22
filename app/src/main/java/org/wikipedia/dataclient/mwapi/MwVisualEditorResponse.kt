package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.wikipedia.json.JsonUtil

@Serializable
class MwVisualEditorResponse : MwResponse() {
    var visualeditor: VisualEditorData? = null

    @Serializable
    class VisualEditorData {
        private val notices: JsonElement? = null

        fun getEditNotices(): Map<String, String>? {
            return if (notices != null && notices is JsonObject) JsonUtil.json.decodeFromJsonElement(notices) else null
        }
    }
}
