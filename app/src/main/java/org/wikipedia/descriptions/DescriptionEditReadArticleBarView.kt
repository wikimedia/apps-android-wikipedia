package org.wikipedia.descriptions

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_description_edit_read_article_bar.view.*
import org.wikipedia.R
import org.wikipedia.dataclient.restbase.page.RbPageSummary
import org.wikipedia.util.L10nUtil.setConditionalLayoutDirection
import org.wikipedia.util.StringUtil

class DescriptionEditReadArticleBarView @JvmOverloads constructor(
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

    fun setPageSummary(pageSummary: RbPageSummary) {
        setConditionalLayoutDirection(this, pageSummary.lang)
        viewArticleTitle!!.text = StringUtil.fromHtml(pageSummary.displayTitle)

        if (TextUtils.isEmpty(pageSummary.thumbnailUrl)) {
            viewArticleImage!!.visibility = GONE
        } else {
            viewArticleImage!!.visibility = VISIBLE
            viewArticleImage!!.loadImage(Uri.parse(pageSummary.thumbnailUrl))
        }

        show()
    }

}
