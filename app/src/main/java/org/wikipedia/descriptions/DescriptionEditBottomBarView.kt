package org.wikipedia.descriptions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_description_edit_read_article_bar.view.*
import org.wikipedia.R
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class DescriptionEditBottomBarView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

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

    fun setSummary(summaryForEdit: PageSummaryForEdit) {
        setConditionalLayoutDirection(this, summaryForEdit.lang)
        viewArticleTitle!!.text = StringUtil.fromHtml(StringUtil.removeNamespace(summaryForEdit.displayTitle!!))
        if (summaryForEdit.thumbnailUrl.isNullOrEmpty()) {
            viewImageThumbnail.visibility = GONE
        } else {
            viewImageThumbnail.visibility = VISIBLE
            ViewUtil.loadImage(viewImageThumbnail, summaryForEdit.thumbnailUrl)
        }
        show()
    }
}
