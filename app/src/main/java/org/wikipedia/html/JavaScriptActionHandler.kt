package org.wikipedia.html

object JavaScriptActionHandler {

    private fun buildJavaScriptString(script: String, value: String): String {
        return "$script = '$value'"
    }

    @JvmStatic
    fun setContentDivTopMargin(topMargin: Float): String {
        return buildJavaScriptString("document.getElementById('content').style.marginTop", topMargin.toString() + "px")
    }

    @JvmStatic
    fun setBodyTopPadding(topMargin: Float): String {
        return buildJavaScriptString("document.body.style.paddingTop", topMargin.toString() + "px")
    }

    @JvmStatic
    fun setBodyBottomPadding(topMargin: Float): String {
        return buildJavaScriptString("document.body.style.paddingBottom", topMargin.toString() + "px")
    }

    @JvmStatic
    fun setTextSelectionAction(): String {
        return "window.getSelection().toString().trim();"
    }
}
