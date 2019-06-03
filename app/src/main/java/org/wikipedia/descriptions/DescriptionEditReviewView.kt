package org.wikipedia.descriptions

import android.content.Context
import android.graphics.drawable.Animatable
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.imagepipeline.image.ImageInfo
import kotlinx.android.synthetic.main.view_description_edit_review.view.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.GradientUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

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

    fun setSummary(summary: SuggestedEditsSummary, description: String, invokeSource: Constants.InvokeSource) {
        L10nUtil.setConditionalLayoutDirection(this, summary.lang)
        if (invokeSource.name.contains("CAPTION")) {
            setGalleryReviewView(summary, description)
        } else {
            setDescriptionReviewView(summary, description)
        }
    }

    private fun setDescriptionReviewView(summary: SuggestedEditsSummary, description: String) {
        galleryContainer.visibility = GONE
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

    private fun setGalleryReviewView(summary: SuggestedEditsSummary, description: String) {
        articleContainer.visibility = GONE
        galleryDescriptionText.text = StringUtil.fromHtml(description)
        galleryDescriptionText.background = GradientUtil.getPowerGradient(R.color.black38, Gravity.TOP)
        galleryImage.hierarchy = GenericDraweeHierarchyBuilder(resources)
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .build()
        if (summary.thumbnailUrl.isNullOrBlank()) {
            galleryImage.visibility = View.GONE
        } else {
            galleryImage.visibility = View.VISIBLE
            galleryImage.setDrawBackground(false)
            // TODO: fix the issue of unable zoom-in / zoom-out
            galleryImage.controller = Fresco.newDraweeControllerBuilder()
                    .setUri(Uri.parse(summary.thumbnailUrl))
                    .setAutoPlayAnimations(true)
                    .setControllerListener(object : BaseControllerListener<ImageInfo>() {
                        override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                            galleryImage.setDrawBackground(true)
                        }

                        override fun onFailure(id: String?, throwable: Throwable?) {
                            L.d(throwable)
                        }
                    })
                    .build()
        }
    }

    companion object {
        const val ARTICLE_EXTRACT_MAX_LINE_WITH_IMAGE = 9
        const val ARTICLE_EXTRACT_MAX_LINE_WITHOUT_IMAGE = 15
    }

}
