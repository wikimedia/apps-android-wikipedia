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

internal class SuggestedEditsDisabledStatesView constructor(context: Context, attrs: AttributeSet? = null) : WikiCardView(context, attrs) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_disabled_states, this)
        learnMoreContainer.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(learnMoreContainer.tag as Int))) }
    }

    fun setPaused(message: String) {
        setDefaultState()
        messageTitleView.text = context.getString(R.string.suggested_edits_paused_title)
        messageTextView.text = StringUtil.fromHtml(message)
        image.setImageResource(R.drawable.ic_suggested_edits_paused)
    }

    fun setDisabled(message: String) {
        setDefaultState()
        messageTitleView.text = context.getString(R.string.suggested_edits_disabled_title)
        messageTextView.text = StringUtil.fromHtml(message)
        image.setImageResource(R.drawable.ic_suggested_edits_disabled)
    }

    fun setIPBlocked() {
        image.visibility = GONE
        messageTitleView.text = context.getString(R.string.suggested_edits_ip_blocked_title)
        messageTextView.text = StringUtil.fromHtml(context.getString(R.string.suggested_edits_ip_blocked_message))
        learnMoreContainer.tag = R.string.create_account_ip_block_help_url
    }

    private fun setDefaultState() {
        image.visibility = View.VISIBLE
        learnMoreContainer.tag = R.string.android_app_edit_help_url
    }
}
