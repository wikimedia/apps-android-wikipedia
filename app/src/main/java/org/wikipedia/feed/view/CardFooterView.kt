package org.wikipedia.feed.view

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_card_footer.view.*
import org.wikipedia.R
import org.wikipedia.util.L10nUtil

internal class CardFooterView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    interface Callback {
        fun onFooterClicked()
    }

    var callback: Callback? = null

    init {
        View.inflate(context, R.layout.view_card_footer, this)
        footerActionButton.setOnClickListener {
            callback?.onFooterClicked()
        }
    }

    fun setFooterActionText(actionText: String) {
        val actionTextWithSpace = "$actionText  "
        val spannableStringBuilder = SpannableStringBuilder(actionTextWithSpace)
        val arrowImageSpan = ImageSpan(context, if (L10nUtil.isDeviceRTL())
            R.drawable.ic_baseline_arrow_left_alt_themed_24px else R.drawable.ic_baseline_arrow_right_alt_themed_24px)
        spannableStringBuilder.setSpan(arrowImageSpan, actionTextWithSpace.length - 1, actionTextWithSpace.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        footerActionButton.text = spannableStringBuilder
    }
}
