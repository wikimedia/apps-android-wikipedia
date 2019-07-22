package org.wikipedia.bridge

import android.content.Context
import org.wikipedia.R

object JavaScriptActionHandler {
    @JvmStatic
    fun setHandler(): String {
        return ("pagelib.c1.InteractionHandling.setInteractionHandler((interaction) => { marshaller.onReceiveMessage(JSON.stringify(interaction))})")
    }

    @JvmStatic
    fun setMargin(context: Context, top: Int, right: Int, bottom: Int, left: Int): String {
        return (context.getString(R.string.page_mh_set_margins_script, top, right, bottom, left))
    }

    @JvmStatic
    fun setScrollTop(context: Context, top: Int): String {
        return (context.getString(R.string.page_mh_set_scrollTop_script, top))
    }

    @JvmStatic
    fun setMulti(context: Context, theme: String, dimImages: Boolean, collapseTables: Boolean): String {
        return context.getString(R.string.page_mh_set_multi_script, theme, dimImages, collapseTables)
    }
}
