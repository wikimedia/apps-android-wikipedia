package org.wikipedia.dataclient.mwapi

class MwVisualEditorResponse : MwResponse() {
    var visualeditor: VisualEditorData? = null

    class VisualEditorData {
        val notices: Map<String, String>? = null
    }
}
