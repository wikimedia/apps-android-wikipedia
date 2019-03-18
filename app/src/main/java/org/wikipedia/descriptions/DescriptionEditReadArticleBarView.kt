package org.wikipedia.descriptions

import android.content.Context
import android.net.Uri
import android.support.constraint.ConstraintLayout
import android.text.TextUtils
import android.util.AttributeSet
import kotlinx.android.synthetic.main.view_description_edit_read_article_bar.view.*
import org.wikipedia.R
import org.wikipedia.dataclient.page.PageSummary
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

    fun setPageSummary(pageSummary: PageSummary, listener: OnClickListener) {
        viewArticleTitle!!.text = StringUtil.fromHtml(pageSummary.displayTitle)
        viewArticleImage!!.loadImage(if (TextUtils.isEmpty(pageSummary.thumbnailUrl)) null else Uri.parse(pageSummary.thumbnailUrl))
        viewReadButton.setOnClickListener(listener)
        show()
    }

}
