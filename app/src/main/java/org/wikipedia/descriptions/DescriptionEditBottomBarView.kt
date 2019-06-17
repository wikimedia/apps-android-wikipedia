package org.wikipedia.descriptions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_description_edit_read_article_bar.view.*
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
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

    fun setSummary(summary: SuggestedEditsSummary, invokeSource: InvokeSource) {
        setConditionalLayoutDirection(this, summary.lang)
        viewArticleTitle!!.text = StringUtil.fromHtml(summary.displayTitle)
        if (invokeSource == InvokeSource.SUGGESTED_EDITS_ADD_CAPTION || invokeSource == InvokeSource.SUGGESTED_EDITS_TRANSLATE_CAPTION) {
            setImageDetails(summary)
        } else {
            setArticleDetails(summary)
        }
        show()
    }

    private fun setArticleDetails(summary: SuggestedEditsSummary) {
        if (summary.thumbnailUrl.isNullOrEmpty()) {
            viewArticleImage!!.visibility = GONE
        } else {
            viewArticleImage!!.visibility = VISIBLE
            ViewUtil.loadImageUrlInto(viewArticleImage!!, summary.thumbnailUrl)
        }

        viewImageThumbnail.visibility = GONE
        viewDownChevron.visibility = GONE
        viewReadButton.visibility = VISIBLE
    }

    private fun setImageDetails(summary: SuggestedEditsSummary) {
        if (summary.thumbnailUrl!!.isEmpty()) {
            viewImageThumbnail!!.visibility = GONE
        } else {
            viewImageThumbnail.visibility = VISIBLE
            ViewUtil.loadImageUrlInto(viewImageThumbnail!!, summary.thumbnailUrl)
        }

        viewDownChevron.visibility = VISIBLE
        viewArticleImage.visibility = GONE
        viewReadButton.visibility = GONE
    }
}
