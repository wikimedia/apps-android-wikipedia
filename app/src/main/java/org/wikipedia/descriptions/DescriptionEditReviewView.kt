package org.wikipedia.descriptions

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_description_edit_review.view.*
import org.wikipedia.R
import org.wikipedia.descriptions.DescriptionEditLicenseView.Companion.ARG_NOTICE_ARTICLE_DESCRIPTION
import org.wikipedia.descriptions.DescriptionEditLicenseView.Companion.ARG_NOTICE_DEFAULT
import org.wikipedia.descriptions.DescriptionEditLicenseView.Companion.ARG_NOTICE_IMAGE_CAPTION
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class DescriptionEditReviewView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    init {
        inflate(context, R.layout.view_description_edit_review, this)
    }

    val isShowing: Boolean
        get() = isVisible

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
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
        galleryContainer.isVisible = false
        articleTitle!!.text = StringUtil.fromHtml(summary.displayTitle)
        articleSubtitle!!.text = description
        articleExtract!!.text = StringUtil.fromHtml(summary.extractHtml)

        val isThumbnailAbsent = summary.thumbnailUrl.isNullOrBlank()
        articleImage.isVisible = !isThumbnailAbsent
        if (isThumbnailAbsent) {
            articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            articleImage.loadImage(summary.getPreferredSizeThumbnailUrl().toUri())
            articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
        }
    }

    private fun setGalleryReviewView(summary: SuggestedEditsSummary, description: String) {
        articleContainer.isVisible = false
        indicatorDivider.isVisible = false
        galleryDescriptionText.text = StringUtil.fromHtml(description)
        val isThumbnailAbsent = summary.thumbnailUrl.isNullOrBlank()
        galleryImage.isVisible = !isThumbnailAbsent
        if (!isThumbnailAbsent) {
            ViewUtil.loadImageWithWhiteBackground(galleryImage, summary.getPreferredSizeThumbnailUrl())
        }
        licenseView.darkLicenseView()
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE = 9
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 15
    }
}
