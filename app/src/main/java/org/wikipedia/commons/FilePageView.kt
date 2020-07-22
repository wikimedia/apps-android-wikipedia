package org.wikipedia.commons

import android.content.Context
import android.icu.text.ListFormatter
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
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
import java.util.*
import kotlin.math.roundToInt

class FilePageView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    init {
        View.inflate(context, R.layout.view_file_page, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun setup(summary: SuggestedEditsSummary,
              imageTags: Map<String, List<String>>,
              containerWidth: Int,
              thumbWidth: Int,
              thumbHeight: Int,
              imageFromCommons: Boolean,
              showFilename: Boolean,
              showEditButton: Boolean,
              action: DescriptionEditActivity.Action? = null) {

        loadImage(summary, containerWidth, thumbWidth, thumbHeight)

        if (showFilename) {
            filenameView.visibility = View.VISIBLE
            filenameView.titleText.text = context.getString(R.string.suggested_edits_image_preview_dialog_file)
            filenameView.titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            filenameView.contentText.text = StringUtil.removeNamespace(summary.displayTitle!!)
            filenameView.contentText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            filenameView.divider.visibility = View.GONE
        }

        detailsContainer.removeAllViews()
        if (summary.pageTitle.description.isNullOrEmpty() && summary.description.isNullOrEmpty()) {

        } else if ((action == DescriptionEditActivity.Action.ADD_CAPTION || action == null) && summary.pageTitle.description.isNullOrEmpty()) {
            // Show the image description when a structured caption does not exist.
            addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_description_in_language_title,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(getProperLanguageCode(summary, imageFromCommons))),
                    summary.description, if (showEditButton) editButtonOnClickListener(summary) else null)
        } else {
            addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_caption_in_language_title,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(getProperLanguageCode(summary, imageFromCommons))),
                    if (summary.pageTitle.description.isNullOrEmpty()) summary.description
                    else summary.pageTitle.description, if (showEditButton) editButtonOnClickListener(summary) else null)
        }
        addDetail(context.getString(R.string.suggested_edits_image_tags), getImageTags(imageTags, getProperLanguageCode(summary, imageFromCommons)))
        addDetail(context.getString(R.string.suggested_edits_image_caption_summary_title_author), summary.metadata!!.artist())
        addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_date), summary.metadata!!.dateTime())
        addDetail(context.getString(R.string.suggested_edits_image_caption_summary_title_source), summary.metadata!!.credit())
        addDetail(true, context.getString(R.string.suggested_edits_image_preview_dialog_licensing), summary.metadata!!.licenseShortName(), summary.metadata!!.licenseUrl())
        if (imageFromCommons) {
            addDetail(false, context.getString(R.string.suggested_edits_image_preview_dialog_more_info), context.getString(R.string.suggested_edits_image_preview_dialog_file_page_link_text), context.getString(R.string.suggested_edits_image_file_page_commons_link, summary.title))
        } else {
            addDetail(false, context.getString(R.string.suggested_edits_image_preview_dialog_more_info), context.getString(R.string.suggested_edits_image_preview_dialog_file_page_wikipedia_link_text), summary.pageTitle.uri)
        }
        requestLayout()
    }

    private fun getImageTags(imageTags: Map<String, List<String>>, languageCode: String) : String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && imageTags.isNotEmpty()) {
            ListFormatter.getInstance(Locale(languageCode)).format(imageTags[languageCode])
        } else {
            imageTags[languageCode]?.joinToString(separator = "\n")
        }
    }

    private fun getProperLanguageCode(summary: SuggestedEditsSummary, imageFromCommons: Boolean): String {
        return if (!imageFromCommons || summary.lang == "commons") {
            WikipediaApp.getInstance().language().appLanguageCode
        } else {
            summary.lang
        }
    }

    private fun loadImage(summary: SuggestedEditsSummary, containerWidth: Int, thumbWidth: Int, thumbHeight: Int) {
        ImageZoomHelper.setViewZoomable(imageView)
        ViewUtil.loadImage(imageView, ImageUrlUtil.getUrlForPreferredSize(summary.thumbnailUrl!!, Constants.PREFERRED_GALLERY_IMAGE_SIZE))
        imageViewPlaceholder.layoutParams = LayoutParams(containerWidth, adjustImagePlaceholderHeight(containerWidth.toFloat(), thumbWidth.toFloat(), thumbHeight.toFloat()))
    }

    private fun editButtonOnClickListener(summary: SuggestedEditsSummary): OnClickListener {
        return OnClickListener {
            startActivity(context, DescriptionEditActivity.newIntent(context,
                    summary.pageTitle, null, summary, null,
                    DescriptionEditActivity.Action.ADD_CAPTION, InvokeSource.FILE_PAGE_ACTIVITY
            ), null)
        }
    }

    private fun adjustImagePlaceholderHeight(containerWidth: Float, thumbWidth: Float, thumbHeight: Float): Int {
        return (Constants.PREFERRED_GALLERY_IMAGE_SIZE.toFloat().div(thumbWidth) * thumbHeight * containerWidth.div(Constants.PREFERRED_GALLERY_IMAGE_SIZE.toFloat())).roundToInt()
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
