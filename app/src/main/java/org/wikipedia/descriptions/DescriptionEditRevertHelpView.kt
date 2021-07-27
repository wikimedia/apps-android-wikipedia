package org.wikipedia.descriptions

import android.content.Context
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.BulletSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ScrollView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewDescriptionEditRevertHelpBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil

class DescriptionEditRevertHelpView constructor(context: Context, attrs: AttributeSet? = null) : ScrollView(context, attrs) {

    private var qNumber: String = ""

    constructor(context: Context, qNumber: String) : this(context, null) {
        this.qNumber = qNumber
    }

    init {
        val binding = ViewDescriptionEditRevertHelpBinding.inflate(LayoutInflater.from(context), this)
        binding.helpText.movementMethod = LinkMovementMethod()
        val helpStr = StringUtil.fromHtml(context.getString(R.string.description_edit_revert_help_body)
                .replace(":revertSubtitle".toRegex(), context.getString(R.string.description_edit_revert_subtitle))
                .replace(":revertIntro".toRegex(), context.getString(R.string.description_edit_revert_intro))
                .replace(":revertHistory".toRegex(), context.getString(R.string.description_edit_revert_history, getHistoryUri(qNumber))))
        val gapWidth = DimenUtil.roundedDpToPx(8f)
        val revertReason1 = SpannableString(StringUtil.fromHtml(context.getString(R.string.description_edit_revert_reason1, context.getString(R.string.wikidata_description_guide_url))))
        val revertReason2 = SpannableString(StringUtil.fromHtml(context.getString(R.string.description_edit_revert_reason2)))
        revertReason1.setSpan(BulletSpan(gapWidth), 0, revertReason1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        revertReason2.setSpan(BulletSpan(gapWidth), 0, revertReason2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.helpText.text = SpannableString(TextUtils.expandTemplate(helpStr, revertReason1, revertReason2))
    }

    private fun getHistoryUri(qNumber: String): Uri {
        return Uri.Builder()
                .scheme(WikipediaApp.instance.wikiSite.scheme())
                .authority("m.wikidata.org")
                .appendPath("wiki")
                .appendPath("Special:History")
                .appendPath(qNumber)
                .build()
    }
}
