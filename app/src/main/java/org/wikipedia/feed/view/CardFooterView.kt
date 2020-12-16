package org.wikipedia.feed.view

import android.annotation.SuppressLint
import android.content.Context
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_card_footer.view.*
import kotlinx.android.synthetic.main.view_on_this_day_event.view.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

internal class CardFooterView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    interface Callback {
        fun onFooterClicked()
    }

    var callback: Callback? = null
    var arrowImageSpan: ImageSpan = ImageSpan(context, if (L10nUtil.isDeviceRTL())
        R.drawable.ic_baseline_arrow_left_alt_themed_24px else R.drawable.ic_baseline_arrow_right_alt_themed_24px)

    init {
        View.inflate(context, R.layout.view_card_footer, this)
        footerActionButton.setOnClickListener {
            callback?.onFooterClicked()
        }
    }

    fun setFooterActionText(actionText: String) {
        val actionTextWithSpace = "$actionText  "
        val spannableStringBuilder = SpannableStringBuilder(actionTextWithSpace)
        spannableStringBuilder.setSpan(arrowImageSpan, actionTextWithSpace.length - 1, actionTextWithSpace.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        footerActionButton.text = spannableStringBuilder
    }
}
