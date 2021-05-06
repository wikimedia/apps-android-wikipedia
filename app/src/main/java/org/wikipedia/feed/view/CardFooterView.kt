package org.wikipedia.feed.view

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewCardFooterBinding
import org.wikipedia.util.L10nUtil

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
        val isRTL = L10nUtil.isLangRTL(langCode ?: WikipediaApp.getInstance().language().systemLanguageCode)
        val arrowImageSpan = ImageSpan(context, if (isRTL) R.drawable.ic_baseline_arrow_left_alt_themed_24px else R.drawable.ic_baseline_arrow_right_alt_themed_24px)
        spannableStringBuilder.setSpan(arrowImageSpan, actionTextWithSpace.length - 1, actionTextWithSpace.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.footerActionButton.text = spannableStringBuilder
    }
}
