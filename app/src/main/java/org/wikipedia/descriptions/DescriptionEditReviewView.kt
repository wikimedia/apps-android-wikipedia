package org.wikipedia.descriptions

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_description_edit_review.view.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.StringUtil

class DescriptionEditReviewView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        inflate(context, R.layout.view_description_edit_review, this)
        orientation = LinearLayout.VERTICAL
    }

    val isShowing: Boolean
        get() = visibility == VISIBLE


    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    fun setPageSummary(pageSummary: PageSummary, description: String) {
        articleTitle!!.text = StringUtil.fromHtml(pageSummary.displayTitle)
        articleSubtitle!!.text = StringUtils.capitalize(description)
        articleExtract!!.text = StringUtil.fromHtml(pageSummary.extractHtml)
        articleImage!!.loadImage(if (TextUtils.isEmpty(pageSummary.thumbnailUrl)) null else Uri.parse(pageSummary.thumbnailUrl))
    }

}
