package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.Serializable

@Serializable
class MwVisualEditorResponse : MwResponse() {
    var visualeditor: VisualEditorData? = null

    @Serializable
    class VisualEditorData {
        val notices: Map<String, String>? = null
    }
}
