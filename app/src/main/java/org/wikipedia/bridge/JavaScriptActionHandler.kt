package org.wikipedia.bridge

object JavaScriptActionHandler {
    @JvmStatic
    fun setHandler(): String {
        return ("pagelib.c1.InteractionHandling.setInteractionHandler((interaction) => { marshaller.onReceiveMessage(JSON.stringify(interaction))})")
    }

    @JvmStatic
    fun setMargin(top: Int, right: Int, bottom: Int, left: Int): String {
        return ("pagelib.c1.PageMods.setMargins(document, { top:'" + top + "px', right:'" + right + "px', bottom:'" + bottom + "px', left:'" + left + "px' })")
    }
}
