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
import org.wikipedia.suggestededits.PageSummaryForEdit
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

    fun setSummary(summaryForEdit: PageSummaryForEdit, description: String, captionReview: Boolean) {
        L10nUtil.setConditionalLayoutDirection(this, summaryForEdit.lang)
        if (captionReview) {
            setGalleryReviewView(summaryForEdit, description)
            licenseView.buildLicenseNotice(ARG_NOTICE_IMAGE_CAPTION)
        } else {
            setDescriptionReviewView(summaryForEdit, description)
            licenseView.buildLicenseNotice(if (summaryForEdit.description.isNullOrEmpty()) ARG_NOTICE_ARTICLE_DESCRIPTION else ARG_NOTICE_DEFAULT)
        }
    }

    private fun setDescriptionReviewView(summaryForEdit: PageSummaryForEdit, description: String) {
        galleryContainer.isVisible = false
        articleTitle!!.text = StringUtil.fromHtml(summaryForEdit.displayTitle)
        articleSubtitle!!.text = description
        articleExtract!!.text = StringUtil.fromHtml(summaryForEdit.extractHtml)

        val isThumbnailAbsent = summaryForEdit.thumbnailUrl.isNullOrBlank()
        articleImage.isVisible = !isThumbnailAbsent
        if (isThumbnailAbsent) {
            articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            articleImage.loadImage(summaryForEdit.getPreferredSizeThumbnailUrl().toUri())
            articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
        }
    }

    private fun setGalleryReviewView(summaryForEdit: PageSummaryForEdit, description: String) {
        articleContainer.isVisible = false
        indicatorDivider.isVisible = false
        galleryDescriptionText.text = StringUtil.fromHtml(description)
        val isThumbnailAbsent = summaryForEdit.thumbnailUrl.isNullOrBlank()
        galleryImage.isVisible = !isThumbnailAbsent
        if (!isThumbnailAbsent) {
            ViewUtil.loadImageWithWhiteBackground(galleryImage, summaryForEdit.getPreferredSizeThumbnailUrl())
        }
        licenseView.darkLicenseView()
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE = 9
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 15
    }
}
