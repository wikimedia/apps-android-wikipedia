package org.wikipedia.feed.configure

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.DefaultViewHolder

class LanguageItemHolder internal constructor(private val context: Context, itemView: View) : DefaultViewHolder<View>(itemView) {
    private val langCodeView: TextView = itemView.findViewById(R.id.feed_content_type_lang_code)
    fun bindItem(langCode: String, enabled: Boolean) {
        langCodeView.text = langCode

        val color = ResourceUtil.getThemedColorStateList(context, R.attr.secondary_color)
        langCodeView.setTextColor(if (enabled) AppCompatResources.getColorStateList(context, android.R.color.white) else color)
        langCodeView.setBackgroundResource(if (enabled) R.drawable.lang_button_shape else R.drawable.lang_button_shape_border)
        ViewCompat.setBackgroundTintList(langCodeView, if (enabled) AppCompatResources.getColorStateList(context, R.color.gray500) else color)
    }
}
