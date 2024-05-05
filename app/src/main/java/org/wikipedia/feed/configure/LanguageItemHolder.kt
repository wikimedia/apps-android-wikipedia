package org.wikipedia.feed.configure

import android.content.Context
import android.view.View
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.DefaultViewHolder
import org.wikipedia.views.LangCodeView

class LanguageItemHolder internal constructor(private val context: Context, private val langCodeView: LangCodeView) : DefaultViewHolder<View>(langCodeView) {
    fun bindItem(langCode: String, enabled: Boolean) {
        langCodeView.setLangCode(langCode)
        val color = ResourceUtil.getThemedColorStateList(context, R.attr.secondary_color)
        langCodeView.setTextColor(if (enabled) ResourceUtil.getThemedColorStateList(context, R.attr.paper_color) else color)
        langCodeView.setBackgroundTint(if (enabled) ResourceUtil.getThemedColorStateList(context, R.attr.placeholder_color) else color)
        langCodeView.fillBackground(enabled)
    }
}
