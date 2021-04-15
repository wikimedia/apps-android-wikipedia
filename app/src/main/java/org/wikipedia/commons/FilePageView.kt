package org.wikipedia.commons

import android.content.Context
import android.icu.text.ListFormatter
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.Constants.PREFERRED_GALLERY_IMAGE_SIZE
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.commons.FilePageFragment.Companion.ACTIVITY_REQUEST_ADD_IMAGE_CAPTION
import org.wikipedia.commons.FilePageFragment.Companion.ACTIVITY_REQUEST_ADD_IMAGE_TAGS
import org.wikipedia.databinding.ViewFilePageBinding
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.ImageDetailView
import org.wikipedia.views.ImageZoomHelper
import org.wikipedia.views.ViewUtil
import java.util.*

class FilePageView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    val binding = ViewFilePageBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun setup(fragment: Fragment,
              summaryForEdit: PageSummaryForEdit,
              imageTags: Map<String, List<String>>,
              page: MwQueryPage,
              containerWidth: Int,
              thumbWidth: Int,
              thumbHeight: Int,
              imageFromCommons: Boolean,
              showFilename: Boolean,
              showEditButton: Boolean,
              suggestionReason: String? = null,
              action: DescriptionEditActivity.Action? = null) {

        loadImage(summaryForEdit, containerWidth, thumbWidth, thumbHeight)

        if (showFilename) {
            binding.filenameView.visibility = View.VISIBLE
            binding.filenameView.binding.titleText.text = context.getString(if (imageFromCommons) R.string.suggested_edits_image_preview_dialog_file_commons else R.string.suggested_edits_image_preview_dialog_image)
            binding.filenameView.binding.contentText.setTextIsSelectable(false)
            binding.filenameView.binding.contentText.maxLines = 3
            binding.filenameView.binding.contentText.ellipsize = TextUtils.TruncateAt.END
            binding.filenameView.binding.contentText.text = StringUtil.removeNamespace(summaryForEdit.displayTitle.orEmpty())
            binding.filenameView.binding.divider.visibility = View.GONE
        }

        binding.detailsContainer.removeAllViews()

        if (summaryForEdit.pageTitle.description.isNullOrEmpty() && summaryForEdit.description.isNullOrEmpty() && showEditButton) {
            addActionButton(context.getString(R.string.file_page_add_image_caption_button), imageCaptionOnClickListener(fragment, summaryForEdit))
        } else if ((action == DescriptionEditActivity.Action.ADD_CAPTION || action == null) && summaryForEdit.pageTitle.description.isNullOrEmpty()) {
            // Show the image description when a structured caption does not exist.
            addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_description_in_language_title,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(getProperLanguageCode(summaryForEdit, imageFromCommons))),
                    summaryForEdit.description, if (showEditButton) imageCaptionOnClickListener(fragment, summaryForEdit) else null)
        } else {
            addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_caption_in_language_title,
                    WikipediaApp.getInstance().language().getAppLanguageLocalizedName(getProperLanguageCode(summaryForEdit, imageFromCommons))),
                    if (summaryForEdit.pageTitle.description.isNullOrEmpty()) summaryForEdit.description
                    else summaryForEdit.pageTitle.description, if (showEditButton) imageCaptionOnClickListener(fragment, summaryForEdit) else null)
        }

        if (!suggestionReason.isNullOrEmpty()) {
            addDetail(context.getString(R.string.file_page_suggestion_reason), suggestionReason.capitalize(Locale.getDefault()))
        }

        if ((imageTags.isNullOrEmpty() || !imageTags.containsKey(getProperLanguageCode(summaryForEdit, imageFromCommons))) && showEditButton) {
            addActionButton(context.getString(R.string.file_page_add_image_tags_button), imageTagsOnClickListener(fragment, page))
        } else {
            addDetail(context.getString(R.string.suggested_edits_image_tags), getImageTags(imageTags, getProperLanguageCode(summaryForEdit, imageFromCommons)))
        }

        addDetail(context.getString(R.string.suggested_edits_image_caption_summary_title_author), summaryForEdit.metadata!!.artist())
        addDetail(context.getString(R.string.suggested_edits_image_preview_dialog_date), summaryForEdit.metadata!!.dateTime())
        addDetail(context.getString(R.string.suggested_edits_image_caption_summary_title_source), summaryForEdit.metadata!!.credit())
        addDetail(true, context.getString(R.string.suggested_edits_image_preview_dialog_licensing), summaryForEdit.metadata!!.licenseShortName(), summaryForEdit.metadata!!.licenseUrl())
        if (imageFromCommons) {
            addDetail(false, context.getString(R.string.suggested_edits_image_preview_dialog_more_info), context.getString(R.string.suggested_edits_image_preview_dialog_file_page_link_text), context.getString(R.string.suggested_edits_image_file_page_commons_link, summaryForEdit.title))
        } else {
            addDetail(false, context.getString(R.string.suggested_edits_image_preview_dialog_more_info), context.getString(R.string.suggested_edits_image_preview_dialog_file_page_wikipedia_link_text), summaryForEdit.pageTitle.uri)
        }
        requestLayout()
    }

    private fun getImageTags(imageTags: Map<String, List<String>>, languageCode: String): String? {
        if (!imageTags.containsKey(languageCode)) {
            return null
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && imageTags.isNotEmpty()) {
            ListFormatter.getInstance(Locale(languageCode)).format(imageTags[languageCode])
        } else {
            imageTags[languageCode]?.joinToString(separator = "\n")
        }
    }

    private fun getProperLanguageCode(summary: PageSummaryForEdit, imageFromCommons: Boolean): String {
        return if (!imageFromCommons || summary.lang == "commons") {
            WikipediaApp.getInstance().language().appLanguageCode
        } else {
            summary.lang
        }
    }

    private fun loadImage(summaryForEdit: PageSummaryForEdit, containerWidth: Int, thumbWidth: Int, thumbHeight: Int) {
        ImageZoomHelper.setViewZoomable(binding.imageView)
        ViewUtil.loadImage(binding.imageView, ImageUrlUtil.getUrlForPreferredSize(summaryForEdit.thumbnailUrl!!, PREFERRED_GALLERY_IMAGE_SIZE), false, false, true, null)
        binding.imageViewPlaceholder.layoutParams = LayoutParams(containerWidth, ViewUtil.adjustImagePlaceholderHeight(containerWidth.toFloat(), thumbWidth.toFloat(), thumbHeight.toFloat()))
    }

    private fun imageCaptionOnClickListener(fragment: Fragment, summaryForEdit: PageSummaryForEdit): OnClickListener {
        return OnClickListener {
            fragment.startActivityForResult(DescriptionEditActivity.newIntent(context,
                    summaryForEdit.pageTitle, null, summaryForEdit, null,
                    DescriptionEditActivity.Action.ADD_CAPTION, InvokeSource.FILE_PAGE_ACTIVITY
            ), ACTIVITY_REQUEST_ADD_IMAGE_CAPTION)
        }
    }

    private fun imageTagsOnClickListener(fragment: Fragment, page: MwQueryPage): OnClickListener {
        return OnClickListener {
            fragment.startActivityForResult(SuggestedEditsImageTagEditActivity.newIntent(context, page, InvokeSource.FILE_PAGE_ACTIVITY),
                    ACTIVITY_REQUEST_ADD_IMAGE_TAGS)
        }
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
            view.binding.titleText.text = titleString
            view.binding.contentText.text = StringUtil.strip(StringUtil.fromHtml(detail))
            RichTextUtil.removeUnderlinesFromLinks(view.binding.contentText)
            if (!externalLink.isNullOrEmpty()) {
                view.binding.contentText.setTextColor(ResourceUtil.getThemedColor(context, R.attr.colorAccent))
                view.binding.contentText.setTextIsSelectable(false)
                view.binding.externalLink.visibility = View.VISIBLE
                view.binding.contentContainer.setOnClickListener {
                    UriUtil.visitInExternalBrowser(context, Uri.parse(externalLink))
                }
            } else {
                view.binding.contentText.movementMethod = movementMethod
            }
            if (!showDivider) {
                view.binding.divider.visibility = View.GONE
            }
            if (listener != null) {
                view.binding.editButton.visibility = View.VISIBLE
                view.binding.editButton.setOnClickListener(listener)
            }
            binding.detailsContainer.addView(view)
        }
    }

    private fun addActionButton(buttonText: String, listener: OnClickListener) {
        val view = ImageDetailView(context)
        view.binding.titleContainer.visibility = View.GONE
        view.binding.contentContainer.visibility = View.GONE
        view.binding.actionButton.visibility = View.VISIBLE
        view.binding.actionButton.text = buttonText
        view.binding.actionButton.setOnClickListener(listener)
        binding.detailsContainer.addView(view)
    }

    private val movementMethod = LinkMovementMethodExt { url: String ->
        UriUtil.handleExternalLink(context, Uri.parse(UriUtil.resolveProtocolRelativeUrl(url)))
    }
}
