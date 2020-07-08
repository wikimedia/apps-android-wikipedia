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
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil

class DescriptionEditReviewView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

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
        galleryContainer.visibility = GONE
        articleTitle!!.text = StringUtil.fromHtml(summaryForEdit.displayTitle)
        articleSubtitle!!.text = description
        articleExtract!!.text = StringUtil.fromHtml(summaryForEdit.extractHtml)

        if (summaryForEdit.thumbnailUrl.isNullOrBlank()) {
            articleImage.visibility = GONE
            articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            articleImage.visibility = VISIBLE
            articleImage.loadImage(Uri.parse(summaryForEdit.getPreferredSizeThumbnailUrl()))
            articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
        }
    }

    private fun setGalleryReviewView(summaryForEdit: PageSummaryForEdit, description: String) {
        articleContainer.visibility = GONE
        indicatorDivider.visibility = GONE
        galleryDescriptionText.text = StringUtil.fromHtml(description)
        if (summaryForEdit.thumbnailUrl.isNullOrBlank()) {
            galleryImage.visibility = GONE
        } else {
            galleryImage.visibility = VISIBLE
            galleryImage.loadImage(Uri.parse(summaryForEdit.getPreferredSizeThumbnailUrl()))
        }
        licenseView.darkLicenseView()
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE = 9
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 15
    }

}
