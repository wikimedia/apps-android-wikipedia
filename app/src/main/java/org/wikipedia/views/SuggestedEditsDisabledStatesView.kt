package org.wikipedia.views

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import kotlinx.android.synthetic.main.view_suggested_edits_disabled_states.view.*
import org.wikipedia.R
import org.wikipedia.analytics.LoginFunnel.SOURCE_SUGGESTED_EDITS
import org.wikipedia.login.LoginActivity
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

internal class SuggestedEditsDisabledStatesView constructor(context: Context, attrs: AttributeSet? = null) : WikiCardView(context, attrs) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_disabled_states, this)
    }

    fun setPaused(message: String) {
        setDefaultState()
        messageTitleView.text = context.getString(R.string.suggested_edits_paused_title)
        messageTextView.text = StringUtil.fromHtml(message)
        imageView.setImageResource(R.drawable.ic_suggested_edits_paused)
    }

    fun setDisabled(message: String) {
        setDefaultState()
        messageTitleView.text = context.getString(R.string.suggested_edits_disabled_title)
        messageTextView.text = StringUtil.fromHtml(message)
        imageView.setImageResource(R.drawable.ic_suggested_edits_disabled)
    }

    fun setIPBlocked() {
        setDefaultState()
        imageView.visibility = GONE
        messageTitleView.text = context.getString(R.string.suggested_edits_ip_blocked_title)
        messageTextView.text = context.getString(R.string.suggested_edits_ip_blocked_message)
        actionButton.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.create_account_ip_block_help_url))) }
        setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.create_account_ip_block_help_url))) }
    }

    fun setRequiredLogin() {
        setDefaultState()
        messageTitleView.text = context.getString(R.string.suggested_edits_encourage_account_creation_title)
        messageTextView.text = context.getString(R.string.suggested_edits_encourage_account_creation_message)
        imageView.setImageResource(R.drawable.ic_require_login_header)
        actionButton.text = context.getString(R.string.suggested_edits_encourage_account_creation_login_button)
        actionButton.icon = null
        actionButton.setOnClickListener { context.startActivity(LoginActivity.newIntent(context, SOURCE_SUGGESTED_EDITS)) }
        disabledStateClickArea.setOnClickListener { context.startActivity(LoginActivity.newIntent(context, SOURCE_SUGGESTED_EDITS)) }
    }

    private fun setDefaultState() {
        imageView.visibility = View.VISIBLE
        actionButton.text = context.getString(R.string.suggested_edits_learn_more)
        actionButton.setIconResource(R.drawable.ic_open_in_new_black_24px)
        actionButton.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.android_app_edit_help_url))) }
        disabledStateClickArea.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.android_app_edit_help_url))) }
    }
}
