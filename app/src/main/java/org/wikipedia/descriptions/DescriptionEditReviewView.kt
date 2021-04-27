package org.wikipedia.descriptions

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.databinding.ViewDescriptionEditReviewBinding
import org.wikipedia.descriptions.DescriptionEditLicenseView.Companion.ARG_NOTICE_ARTICLE_DESCRIPTION
import org.wikipedia.descriptions.DescriptionEditLicenseView.Companion.ARG_NOTICE_DEFAULT
import org.wikipedia.descriptions.DescriptionEditLicenseView.Companion.ARG_NOTICE_IMAGE_CAPTION
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewUtil

class DescriptionEditReviewView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    private val binding = ViewDescriptionEditReviewBinding.inflate(LayoutInflater.from(context), this)

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
            binding.licenseView.buildLicenseNotice(ARG_NOTICE_IMAGE_CAPTION)
        } else {
            setDescriptionReviewView(summaryForEdit, description)
            binding.licenseView.buildLicenseNotice(if (summaryForEdit.description.isNullOrEmpty()) ARG_NOTICE_ARTICLE_DESCRIPTION else ARG_NOTICE_DEFAULT,
                    summaryForEdit.lang)
        }
    }

    private fun setDescriptionReviewView(summaryForEdit: PageSummaryForEdit, description: String) {
        binding.galleryContainer.visibility = GONE
        binding.articleTitle.text = StringUtil.fromHtml(summaryForEdit.displayTitle)
        binding.articleSubtitle.text = description
        binding.articleExtract.text = StringUtil.fromHtml(summaryForEdit.extractHtml)

        if (summaryForEdit.thumbnailUrl.isNullOrBlank()) {
            binding.articleImage.visibility = GONE
            binding.articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE
        } else {
            binding.articleImage.visibility = VISIBLE
            binding.articleImage.loadImage(Uri.parse(summaryForEdit.getPreferredSizeThumbnailUrl()))
            binding.articleExtract.maxLines = ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE
        }
    }

    private fun setGalleryReviewView(summaryForEdit: PageSummaryForEdit, description: String) {
        binding.articleContainer.visibility = GONE
        binding.indicatorDivider.visibility = GONE
        binding.galleryDescriptionText.text = StringUtil.fromHtml(description)
        if (summaryForEdit.thumbnailUrl.isNullOrBlank()) {
            binding.galleryImage.visibility = GONE
        } else {
            binding.galleryImage.visibility = VISIBLE
            ViewUtil.loadImage(binding.galleryImage, summaryForEdit.getPreferredSizeThumbnailUrl())
        }
        binding.licenseView.darkLicenseView()
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE = 5
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 15
    }
}
