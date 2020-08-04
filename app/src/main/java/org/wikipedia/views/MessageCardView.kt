package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.view_message_card.view.*
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.LoginFunnel.SOURCE_SUGGESTED_EDITS
import org.wikipedia.login.LoginActivity
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

internal class MessageCardView constructor(context: Context, attrs: AttributeSet? = null) : WikiCardView(context, attrs) {

    init {
        View.inflate(context, R.layout.view_message_card, this)
    }

    fun setMessageTitle(title: String) {
        messageTitleView.text = title
    }

    fun setMessageText(text: String) {
        messageTextView.text = text
    }

    fun setImageResource(@DrawableRes imageResource: Int, visible: Boolean) {
        if (visible) {
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(imageResource)
        } else {
            imageView.visibility = View.GONE
        }
    }

    fun setPositiveButton(@StringRes stringRes: Int, listener: OnClickListener, applyListenerToContainer: Boolean) {
        positiveButton.text = context.getString(stringRes)
        positiveButton.setOnClickListener(listener)
        if (applyListenerToContainer) {
            containerClickArea.setOnClickListener(listener)
        }
    }

    fun setNegativeButton(@StringRes stringRes: Int, listener: OnClickListener, applyListenerToContainer: Boolean) {
        negativeButton.text = context.getString(stringRes)
        negativeButton.setOnClickListener(listener)
        negativeButton.visibility = View.VISIBLE
        if (applyListenerToContainer) {
            containerClickArea.setOnClickListener(listener)
        }
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
        positiveButton.setOnClickListener { UriUtil.visitInExternalBrowser(context, context.getString(R.string.create_account_ip_block_help_url).toUri()) }
        setOnClickListener { UriUtil.visitInExternalBrowser(context, context.getString(R.string.create_account_ip_block_help_url).toUri()) }
    }

    fun setRequiredLogin(fragment: Fragment) {
        imageView.visibility = View.VISIBLE
        messageTitleView.text = context.getString(R.string.suggested_edits_encourage_account_creation_title)
        messageTextView.text = context.getString(R.string.suggested_edits_encourage_account_creation_message)
        imageView.setImageResource(R.drawable.ic_require_login_header)
        positiveButton.text = context.getString(R.string.suggested_edits_encourage_account_creation_login_button)
        positiveButton.setOnClickListener { fragment.startActivityForResult(LoginActivity.newIntent(context, SOURCE_SUGGESTED_EDITS), Constants.ACTIVITY_REQUEST_LOGIN) }
        containerClickArea.setOnClickListener { fragment.startActivityForResult(LoginActivity.newIntent(context, SOURCE_SUGGESTED_EDITS), Constants.ACTIVITY_REQUEST_LOGIN) }
    }

    private fun setDefaultState() {
        imageView.visibility = View.VISIBLE
        positiveButton.text = context.getString(R.string.suggested_edits_learn_more)
        positiveButton.setIconResource(R.drawable.ic_open_in_new_black_24px)
        positiveButton.setOnClickListener { UriUtil.visitInExternalBrowser(context, context.getString(R.string.android_app_edit_help_url).toUri()) }
        containerClickArea.setOnClickListener { UriUtil.visitInExternalBrowser(context, context.getString(R.string.android_app_edit_help_url).toUri()) }
    }
}
