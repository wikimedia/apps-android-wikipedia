package org.wikipedia.commons

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.startActivity
import kotlinx.android.synthetic.main.view_file_page.view.*
import kotlinx.android.synthetic.main.view_image_detail.view.*
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.suggestededits.SuggestedEditsSummary
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.ImageDetailView
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.ViewUtil

class FilePageView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.view_file_page, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun setup(summary: SuggestedEditsSummary,
              action: DescriptionEditActivity.Action,
              containerWidth: Int,
              thumbWidth: Int,
              thumbHeight: Int,
              imageFromCommons: Boolean) {

        detailsContainer.removeAllViews()
        ImageZoomHelper.setViewZoomable(imageView)
        ViewUtil.loadImage(imageView, ImageUrlUtil.getUrlForPreferredSize(summary.thumbnailUrl!!, Constants.PREFERRED_GALLERY_IMAGE_SIZE))
        imageViewPlaceholder.layoutParams = LayoutParams(containerWidth, adjustImagePlaceholderHeight(containerWidth, thumbWidth, thumbHeight))

        var appLanguageLocalizedName = WikipediaApp.getInstance().language().getAppLanguageLocalizedName(summary.lang)
        if (!imageFromCommons) {
            appLanguageLocalizedName = WikipediaApp.getInstance().language().getAppLanguageLocalizedName(WikipediaApp.getInstance().language().appLanguageCode)
        }

        if ((action == DescriptionEditActivity.Action.ADD_CAPTION)
                && summary.pageTitle.description.isNullOrEmpty()) {
            // Show the image description when a structured caption does not exist.
            addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_description_in_language_title, appLanguageLocalizedName),
                    summary.description, editButtonOnClickListener(summary))
        } else {
            addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_caption_in_language_title, appLanguageLocalizedName),
                    if (summary.pageTitle.description.isNullOrEmpty()) summary.description
                    else summary.pageTitle.description, editButtonOnClickListener(summary))
        }
        addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_artist), summary.metadata!!.artist())
        addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_date), summary.metadata!!.dateTime())
        addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_source), summary.metadata!!.credit())
        addDetail(true, context.getString(R.string.suggested_edits_image_preview_dialog_licensing), summary.metadata!!.licenseShortName(), summary.metadata!!.licenseUrl())
        if (imageFromCommons) {
            addDetail(false, context.getString(R.string.suggested_edits_image_preview_dialog_more_info), context.getString(R.string.suggested_edits_image_preview_dialog_file_page_link_text), context.getString(R.string.suggested_edits_image_file_page_commons_link, summary.title))
        } else {
            addDetail(false, context.getString(R.string.suggested_edits_image_preview_dialog_more_info), context.getString(R.string.suggested_edits_image_preview_dialog_file_page_wikipedia_link_text), summary.pageTitle.uri)
        }
        requestLayout()
    }

    private fun editButtonOnClickListener(summary: SuggestedEditsSummary): OnClickListener {
        return OnClickListener {
            startActivity(context, DescriptionEditActivity.newIntent(context,
                    summary.pageTitle, null, summary, null,
                    DescriptionEditActivity.Action.ADD_CAPTION, InvokeSource.FILE_PAGE_ACTIVITY
            ), null)
        }
    }

    private fun adjustImagePlaceholderHeight(containerWidth: Int, thumbWidth: Int, thumbHeight: Int): Int {
        var placeholderHeight = if (Constants.PREFERRED_GALLERY_IMAGE_SIZE > thumbWidth) {
            Constants.PREFERRED_GALLERY_IMAGE_SIZE / thumbWidth * thumbHeight
        } else {
            thumbWidth / Constants.PREFERRED_GALLERY_IMAGE_SIZE * thumbHeight
        }
        placeholderHeight *= if (containerWidth > Constants.PREFERRED_GALLERY_IMAGE_SIZE) {
            containerWidth / Constants.PREFERRED_GALLERY_IMAGE_SIZE
        } else {
            Constants.PREFERRED_GALLERY_IMAGE_SIZE / containerWidth
        }
        return placeholderHeight
    }

    private fun addDetail(titleString: String, detail: String?) {
        addDetail(true, titleString, detail, null, null)
    }

    private fun addDetail(titleString: String, detail: String?, listener: OnClickListener?) {
        addDetail(true, titleString, detail, null, listener)
    }

    private fun addDetail(showDivider: Boolean, titleString: String, detail: String?, externalLink: String?) {
        addDetail(showDivider, titleString, detail, externalLink, null)
    }

    private fun addDetail(showDivider: Boolean, titleString: String, detail: String?, externalLink: String?, listener: OnClickListener?) {
        if (!detail.isNullOrEmpty()) {
            val view = ImageDetailView(context)
            view.titleText.text = titleString
            view.contentText.text = StringUtil.strip(StringUtil.fromHtml(detail))
            RichTextUtil.removeUnderlinesFromLinks(view.contentText)
            if (!externalLink.isNullOrEmpty()) {
                view.contentText.setTextColor(ResourceUtil.getThemedColor(context, R.attr.colorAccent))
                view.contentText.setTextIsSelectable(false)
                view.externalLink.visibility = View.VISIBLE
                view.contentContainer.setOnClickListener {
                    UriUtil.visitInExternalBrowser(context, Uri.parse(externalLink))
                }
            } else {
                view.contentText.movementMethod = movementMethod
            }
            if (!showDivider) {
                view.divider.visibility = View.GONE
            }
            if (listener != null) {
                view.editButton.visibility = View.VISIBLE
                view.editButton.setOnClickListener(listener)
            }
            detailsContainer.addView(view)
        }
    }

    private val movementMethod = LinkMovementMethodExt { url: String ->
        UriUtil.handleExternalLink(context, Uri.parse(UriUtil.resolveProtocolRelativeUrl(url)))
    }

}
