package org.wikipedia.descriptions

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.view_description_edit_license.view.*
import org.wikipedia.R
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.StringUtil

class DescriptionEditLicenseView  @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        inflate(context, R.layout.view_description_edit_license, this)
        licenseText.movementMethod = LinkMovementMethod()
        buildLicenseNotice(ARG_NOTICE_DEFAULT)
    }

    fun buildLicenseNotice(arg: String) {
        licenseText.text = StringUtil.fromHtml(context.getString(getLicenseTextRes(arg),
                context.getString(R.string.terms_of_use_url), context.getString(R.string.cc_0_url)))
        RichTextUtil.removeUnderlinesFromLinks(licenseText)
    }

    fun darkLicenseView() {
        val white70 = ContextCompat.getColor(context, R.color.white70)
        setBackgroundResource(android.R.color.black)
        licenseText.setTextColor(white70)
        licenseText.setLinkTextColor(white70)
        licenseIcon.setColorFilter(white70, android.graphics.PorterDuff.Mode.SRC_IN)
    }

    private fun getLicenseTextRes(arg: String): Int =
            when(arg) {
                ARG_NOTICE_ARTICLE_DESCRIPTION -> R.string.suggested_edits_license_notice
                ARG_NOTICE_IMAGE_CAPTION -> R.string.suggested_edits_image_caption_license_notice
                else -> R.string.description_edit_license_notice
            }

    companion object {
        const val ARG_NOTICE_DEFAULT = "defaultNotice"
        const val ARG_NOTICE_IMAGE_CAPTION = "imageCaptionNotice"
        const val ARG_NOTICE_ARTICLE_DESCRIPTION = "articleDescriptionNotice"
    }
}
