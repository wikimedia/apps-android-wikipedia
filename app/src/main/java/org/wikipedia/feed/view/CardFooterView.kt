package org.wikipedia.feed.view

import android.content.Context
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewCardFooterBinding
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil

class CardFooterView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    fun interface Callback {
        fun onFooterClicked()
    }

    private val binding = ViewCardFooterBinding.inflate(LayoutInflater.from(context), this)
    var callback: Callback? = null

    init {
        binding.footerActionButton.setOnClickListener {
            callback?.onFooterClicked()
        }
    }

    fun setFooterActionText(actionText: String, langCode: String?) {
        val isRTL = L10nUtil.isLangRTL(langCode ?: WikipediaApp.instance.languageState.systemLanguageCode)
        val iconColor = ResourceUtil.getThemedColor(context, R.attr.progressive_color)
        // TODO: revisit this after the ImageSpan can render drawable instead of converting it to bitmap.
        val arrowLeftDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_arrow_left_alt_24px)!!
            .apply { setTint(iconColor) }
            .toBitmap()
        val arrowRightDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_arrow_right_alt_24px)!!
            .apply { setTint(iconColor) }
            .toBitmap()
        binding.footerActionButton.text = buildSpannedString {
            append("$actionText ")
            inSpans(ImageSpan(context, if (isRTL) arrowLeftDrawable else arrowRightDrawable)) {
                append(" ")
            }
        }
    }
}
