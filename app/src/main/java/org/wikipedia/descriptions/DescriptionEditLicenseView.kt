package org.wikipedia.descriptions

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.ViewDescriptionEditLicenseBinding
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

class DescriptionEditLicenseView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    fun interface Callback {
        fun onLoginClick()
    }

    var callback: Callback? = null
    private val binding = ViewDescriptionEditLicenseBinding.inflate(LayoutInflater.from(context), this)
    private val movementMethod = LinkMovementMethodExt { url: String ->
        if (url == "https://#login") {
            callback?.onLoginClick()
        } else {
            UriUtil.handleExternalLink(context, url.toUri())
        }
    }

    init {
        orientation = VERTICAL
        if (!isInEditMode) {
            binding.licenseText.movementMethod = movementMethod
            binding.anonWarningText.movementMethod = movementMethod
            buildLicenseNotice(ARG_NOTICE_DEFAULT)
        }
    }

    fun buildLicenseNotice(arg: String, lang: String? = null) {
        if ((arg == ARG_NOTICE_ARTICLE_DESCRIPTION || arg == ARG_NOTICE_DEFAULT) &&
                DescriptionEditUtil.wikiUsesLocalDescriptions(lang.orEmpty())) {
            binding.licenseText.text = StringUtil.fromHtml(context.getString(R.string.edit_save_action_license_logged_in,
                    context.getString(R.string.terms_of_use_url),
                    context.getString(R.string.cc_by_sa_4_url)))
        } else {
            binding.licenseText.text = StringUtil.fromHtml(context.getString(when (arg) {
                ARG_NOTICE_ARTICLE_DESCRIPTION -> R.string.suggested_edits_license_notice
                ARG_NOTICE_IMAGE_CAPTION -> R.string.suggested_edits_image_caption_license_notice
                else -> R.string.description_edit_license_notice
            }, context.getString(R.string.terms_of_use_url), context.getString(R.string.cc_0_url)))
        }
        binding.anonWarningText.text = StringUtil.fromHtml(context.getString(R.string.edit_anon_warning))
        binding.anonWarningText.isVisible = !AccountUtil.isLoggedIn
    }

    fun darkLicenseView() {
        val licenseColor = ResourceUtil.getThemedColorStateList(context, R.attr.placeholder_color)
        setBackgroundResource(android.R.color.black)
        binding.licenseText.setTextColor(licenseColor)
        binding.licenseText.setLinkTextColor(licenseColor)
        TextViewCompat.setCompoundDrawableTintList(binding.licenseText, licenseColor)
        binding.anonWarningText.setTextColor(licenseColor)
        binding.anonWarningText.setLinkTextColor(licenseColor)
    }

    companion object {
        const val ARG_NOTICE_DEFAULT = "defaultNotice"
        const val ARG_NOTICE_IMAGE_CAPTION = "imageCaptionNotice"
        const val ARG_NOTICE_ARTICLE_DESCRIPTION = "articleDescriptionNotice"
    }
}
