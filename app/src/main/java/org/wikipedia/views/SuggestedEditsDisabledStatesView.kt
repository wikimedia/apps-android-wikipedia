package org.wikipedia.views

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_suggested_edits_disabled_states.view.*
import org.wikipedia.R
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

internal class SuggestedEditsDisabledStatesView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_disabled_states, this)
        linkContainer1.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(linkContainer1.tag as Int))) }
    }

    fun setPaused(message: String) {
        setDefaultState()
        messageTextView.text = StringUtil.fromHtml(message)
        image.setImageResource(R.drawable.ic_suggested_edits_paused)
    }

    fun setDisabled(message: String) {
        setDefaultState()
        messageTextView.text = StringUtil.fromHtml(message)
        image.setImageResource(R.drawable.ic_suggested_edits_disabled)
    }

    fun setIPBlocked() {
        image.visibility = GONE
        messageTextView.text = StringUtil.fromHtml(context.getString(R.string.suggested_edits_ip_blocked_message))

        linkText1.text = context.getString(R.string.suggested_edits_help_page_link_text)
        linkContainer1.tag = R.string.create_account_ip_block_help_url
    }

    private fun setDefaultState() {
        image.visibility = View.VISIBLE
        linkText1.text = context.getString(R.string.suggested_edits_help_page_link_text)
        linkContainer1.tag = R.string.android_app_edit_help_url
    }
}
