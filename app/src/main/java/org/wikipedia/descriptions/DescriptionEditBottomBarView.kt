package org.wikipedia.descriptions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_description_edit_read_article_bar.view.*
import org.wikipedia.R
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class DescriptionEditBottomBarView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.view_description_edit_read_article_bar, this)
        isVisible = false
    }

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }

    fun setSummary(summary: SuggestedEditsSummary) {
        setConditionalLayoutDirection(this, summary.lang)
        viewArticleTitle!!.text = StringUtil.fromHtml(StringUtil.removeNamespace(summary.displayTitle!!))
        val isThumbnailAbsent = summary.thumbnailUrl.isNullOrEmpty()
        viewImageThumbnail.isVisible = !isThumbnailAbsent
        if (!isThumbnailAbsent) {
            ViewUtil.loadImage(viewImageThumbnail, summary.thumbnailUrl)
        }
        isVisible = true
    }
}
