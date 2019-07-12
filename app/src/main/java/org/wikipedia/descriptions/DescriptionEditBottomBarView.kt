package org.wikipedia.descriptions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_description_edit_read_article_bar.view.*
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

    fun setSummary(summary: SuggestedEditsSummary) {
        setConditionalLayoutDirection(this, summary.lang)
        viewArticleTitle!!.text = StringUtil.fromHtml(StringUtil.removeNamespace(summary.displayTitle!!))
        if (summary.thumbnailUrl.isNullOrEmpty()) {
            viewImageThumbnail.visibility = GONE
        } else {
            viewImageThumbnail.visibility = VISIBLE
            ViewUtil.loadImageUrlInto(viewImageThumbnail, summary.thumbnailUrl)
        }
        show()
    }
}
