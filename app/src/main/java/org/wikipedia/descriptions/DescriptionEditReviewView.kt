package org.wikipedia.descriptions

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_description_edit_review.view.*
import org.wikipedia.R
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil

class DescriptionEditReviewView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : ConstraintLayout(context, attrs, defStyle) {

    init {
        inflate(context, R.layout.view_description_edit_review, this)
        licenseView.buildLicenseNotice(false)
        licenseView.removeUnderlinesFromLinks()
    }

    val isShowing: Boolean
        get() = visibility == VISIBLE


    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    fun setSummary(summary: SuggestedEditsSummary, description: String) {
        L10nUtil.setConditionalLayoutDirection(this, summary.lang)
        articleTitle!!.text = StringUtil.fromHtml(summary.displayTitle)
        articleSubtitle!!.text = description.capitalize()
        articleExtract!!.text = StringUtil.fromHtml(summary.extractHtml)

        if (summary.thumbnailUrl.isNullOrBlank()) {
            articleImage.visibility = View.GONE
            articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            articleImage.visibility = View.VISIBLE
            articleImage.loadImage(Uri.parse(summary.thumbnailUrl))
            articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
        }
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE = 9
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 15
    }

}
