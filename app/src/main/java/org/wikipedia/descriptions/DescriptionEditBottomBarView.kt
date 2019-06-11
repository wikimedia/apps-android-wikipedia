package org.wikipedia.descriptions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_description_edit_read_article_bar.view.*
import org.wikipedia.R
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil


class DescriptionEditBottomBarView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : ConstraintLayout(context, attrs, defStyle) {

    init {
        inflate(context, R.layout.view_description_edit_read_article_bar, this)
        hide()
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    fun setSummary(summary: SuggestedEditsSummary) {
        setConditionalLayoutDirection(this, summary.lang)
        val horizontalMargin: Int = DimenUtil.dpToPx(16.0f).toInt()
        val params: MarginLayoutParams = viewArticleTitle!!.layoutParams as MarginLayoutParams
        params.setMargins(horizontalMargin, 0, 0, 0)
        viewArticleTitle!!.layoutParams = params
        viewArticleTitle!!.requestLayout()
        viewArticleTitle!!.text = StringUtil.fromHtml(summary.displayTitle)

        if (summary.thumbnailUrl.isNullOrEmpty()) {
            viewArticleImage!!.visibility = GONE
        } else {
            val verticalMargin: Int = DimenUtil.dpToPx(8.0f).toInt()
            val layoutparams: MarginLayoutParams = viewArticleImage.layoutParams as MarginLayoutParams
            layoutparams.setMargins(horizontalMargin, verticalMargin, 0, verticalMargin)
            viewArticleImage!!.layoutParams = layoutparams
            viewArticleImage!!.requestLayout()
            val padding: Int = DimenUtil.dpToPx(4.0f).toInt()
            viewArticleImage!!.setPadding(padding, padding, padding, padding)
            viewArticleImage!!.visibility = VISIBLE
            ViewUtil.loadImageUrlInto(viewArticleImage!!, summary.thumbnailUrl)
        }

        viewImageThumbnail.visibility = View.GONE
        viewReadButton.visibility = View.VISIBLE
        show()
    }

    fun setImageDetails(suggestedEditsSummary: SuggestedEditsSummary) {
        setConditionalLayoutDirection(this, suggestedEditsSummary.lang)
        viewArticleTitle!!.text = suggestedEditsSummary.title
        val titlePadding: Int = DimenUtil.dpToPx(62.0f).toInt()
        viewArticleTitle!!.setPadding(0, 0, titlePadding, 0)

        val padding: Int = DimenUtil.dpToPx(12.0f).toInt()
        viewArticleImage!!.visibility = VISIBLE
        viewArticleImage!!.setPadding(padding, padding, padding, padding)
        viewArticleImage!!.setColorFilter(ResourceUtil.getThemedColor(context, R.attr.main_toolbar_icon_color))
        viewArticleImage.setImageResource(R.drawable.ic_keyboard_arrow_up_black_24dp)

        if (suggestedEditsSummary.thumbnailUrl!!.isEmpty()) {
            viewImageThumbnail!!.visibility = GONE
        } else {
            viewImageThumbnail.visibility = View.VISIBLE
            ViewUtil.loadImageUrlInto(viewImageThumbnail!!, suggestedEditsSummary.thumbnailUrl)
        }

        viewReadButton.visibility = View.GONE
        show()
    }

}
