package org.wikipedia.feed.view

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.toBitmap
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
        val actionTextWithSpace = "$actionText  "
        val spannableStringBuilder = SpannableStringBuilder(actionTextWithSpace)
        val isRTL = L10nUtil.isLangRTL(langCode ?: WikipediaApp.instance.languageState.systemLanguageCode)
        val iconColor = ResourceUtil.getThemedColor(context, R.attr.progressive_color)
        // TODO: revisit this after the ImageSpan can render drawable instead of converting it to bitmap.
        val arrowLeftDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_arrow_left_alt_24px)?.apply {
            setTint(iconColor)
        }?.toBitmap()!!
        val arrowRightDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_baseline_arrow_right_alt_24px)?.apply {
            setTint(iconColor)
        }?.toBitmap()!!
        val arrowImageSpan = ImageSpan(context, if (isRTL) arrowLeftDrawable else arrowRightDrawable)
        spannableStringBuilder.setSpan(arrowImageSpan, actionTextWithSpace.length - 1, actionTextWithSpace.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.footerActionButton.text = spannableStringBuilder
    }
}
