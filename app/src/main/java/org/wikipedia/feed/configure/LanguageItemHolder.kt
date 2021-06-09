package org.wikipedia.feed.configure

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import org.wikipedia.R
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.DefaultViewHolder

class LanguageItemHolder internal constructor(private val context: Context, itemView: View) : DefaultViewHolder<View>(itemView) {
    private val langCodeView: TextView = itemView.findViewById(R.id.feed_content_type_lang_code)
    fun bindItem(langCode: String, enabled: Boolean) {
        langCodeView.text = langCode

        langCodeView.setTextColor(if (enabled) ContextCompat.getColor(context, android.R.color.white)
        else ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color))

        langCodeView.background = AppCompatResources.getDrawable(context, if (enabled) R.drawable.lang_button_shape
        else R.drawable.lang_button_shape_border)

        langCodeView.background.colorFilter = BlendModeColorFilterCompat
            .createBlendModeColorFilterCompat(if (enabled) ContextCompat.getColor(context, R.color.base30)
            else ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color), BlendModeCompat.SRC_IN)
    }
}
