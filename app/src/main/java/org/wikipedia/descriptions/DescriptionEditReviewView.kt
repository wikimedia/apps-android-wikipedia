package org.wikipedia.descriptions

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.view_description_edit_review.view.*
import org.wikipedia.R
import org.wikipedia.descriptions.DescriptionEditLicenseView.Companion.ARG_NOTICE_ARTICLE_DESCRIPTION
import org.wikipedia.descriptions.DescriptionEditLicenseView.Companion.ARG_NOTICE_DEFAULT
import org.wikipedia.descriptions.DescriptionEditLicenseView.Companion.ARG_NOTICE_IMAGE_CAPTION
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil

class DescriptionEditReviewView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : ConstraintLayout(context, attrs, defStyle) {

    init {
        inflate(context, R.layout.view_description_edit_review, this)
    }

    val isShowing: Boolean
        get() = visibility == VISIBLE


    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    fun setSummary(summary: SuggestedEditsSummary, description: String, captionReview: Boolean) {
        L10nUtil.setConditionalLayoutDirection(this, summary.lang)
        if (captionReview) {
            setGalleryReviewView(summary, description)
            licenseView.buildLicenseNotice(ARG_NOTICE_IMAGE_CAPTION)
        } else {
            setDescriptionReviewView(summary, description)
            licenseView.buildLicenseNotice(if (summary.description.isNullOrEmpty()) ARG_NOTICE_ARTICLE_DESCRIPTION else ARG_NOTICE_DEFAULT)
        }
    }

    private fun setDescriptionReviewView(summary: SuggestedEditsSummary, description: String) {
        galleryContainer.visibility = GONE
        articleTitle!!.text = StringUtil.fromHtml(summary.displayTitle)
        articleSubtitle!!.text = description.capitalize()
        articleExtract!!.text = StringUtil.fromHtml(summary.extractHtml)

        if (summary.thumbnailUrl.isNullOrBlank()) {
            articleImage.visibility = GONE
            articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            articleImage.visibility = VISIBLE
            articleImage.loadImage(Uri.parse(summary.getPreferredSizeThumbnailUrl()))
            articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
        }
    }

    private fun setGalleryReviewView(summary: SuggestedEditsSummary, description: String) {
        articleContainer.visibility = GONE
        indicatorDivider.visibility = GONE
        galleryDescriptionText.text = StringUtil.fromHtml(description)
        if (summary.thumbnailUrl.isNullOrBlank()) {
            galleryImage.visibility = GONE
        } else {
            galleryImage.visibility = VISIBLE
            galleryImage.loadImage(Uri.parse(summary.getPreferredSizeThumbnailUrl()))
        }
        licenseView.darkLicenseView()
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE = 9
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 15
    }

}
