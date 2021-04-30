package org.wikipedia.descriptions

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.databinding.ViewDescriptionEditLicenseBinding
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.StringUtil

class DescriptionEditLicenseView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding = ViewDescriptionEditLicenseBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.licenseText.movementMethod = LinkMovementMethod.getInstance()
        buildLicenseNotice(ARG_NOTICE_DEFAULT)
    }

    fun buildLicenseNotice(arg: String, lang: String? = null) {
        if ((arg == ARG_NOTICE_ARTICLE_DESCRIPTION || arg == ARG_NOTICE_DEFAULT) &&
                DescriptionEditFragment.wikiUsesLocalDescriptions(lang.orEmpty())) {
            binding.licenseText.text = StringUtil.fromHtml(context.getString(R.string.edit_save_action_license_logged_in,
                    context.getString(R.string.terms_of_use_url),
                    context.getString(R.string.cc_by_sa_3_url)))
        } else {
            binding.licenseText.text = StringUtil.fromHtml(context.getString(when (arg) {
                ARG_NOTICE_ARTICLE_DESCRIPTION -> R.string.suggested_edits_license_notice
                ARG_NOTICE_IMAGE_CAPTION -> R.string.suggested_edits_image_caption_license_notice
                else -> R.string.description_edit_license_notice
            }, context.getString(R.string.terms_of_use_url), context.getString(R.string.cc_0_url)))
        }

        RichTextUtil.removeUnderlinesFromLinks(binding.licenseText)
    }

    fun darkLicenseView() {
        val white70 = ContextCompat.getColor(context, R.color.white70)
        setBackgroundResource(android.R.color.black)
        binding.licenseText.setTextColor(white70)
        binding.licenseText.setLinkTextColor(white70)
        binding.licenseIcon.setColorFilter(white70, android.graphics.PorterDuff.Mode.SRC_IN)
    }

    companion object {
        const val ARG_NOTICE_DEFAULT = "defaultNotice"
        const val ARG_NOTICE_IMAGE_CAPTION = "imageCaptionNotice"
        const val ARG_NOTICE_ARTICLE_DESCRIPTION = "articleDescriptionNotice"
    }
}
