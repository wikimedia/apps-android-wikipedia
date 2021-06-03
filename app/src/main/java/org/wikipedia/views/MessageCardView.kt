package org.wikipedia.views

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.LoginFunnel.Companion.SOURCE_SUGGESTED_EDITS
import org.wikipedia.databinding.ViewMessageCardBinding
import org.wikipedia.login.LoginActivity
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

class MessageCardView constructor(context: Context, attrs: AttributeSet? = null) : WikiCardView(context, attrs) {

    val binding = ViewMessageCardBinding.inflate(LayoutInflater.from(context), this, true)

    fun setMessageTitle(title: String) {
        binding.messageTitleView.text = title
    }

    fun setMessageText(text: String) {
        binding.messageTextView.text = text
    }

    fun setImageResource(@DrawableRes imageResource: Int, visible: Boolean) {
        if (visible) {
            binding.imageView.visibility = View.VISIBLE
            binding.imageView.setImageResource(imageResource)
        } else {
            binding.imageView.visibility = View.GONE
        }
    }

    fun setPositiveButton(@StringRes stringRes: Int, listener: OnClickListener, applyListenerToContainer: Boolean) {
        binding.positiveButton.text = context.getString(stringRes)
        binding.positiveButton.setOnClickListener(listener)
        if (applyListenerToContainer) {
            binding.containerClickArea.setOnClickListener(listener)
        }
    }

    fun setNegativeButton(@StringRes stringRes: Int, listener: OnClickListener, applyListenerToContainer: Boolean) {
        binding.negativeButton.text = context.getString(stringRes)
        binding.negativeButton.setOnClickListener(listener)
        binding.negativeButton.visibility = View.VISIBLE
        if (applyListenerToContainer) {
            binding.containerClickArea.setOnClickListener(listener)
        }
    }

    fun setPaused(message: String) {
        setDefaultState()
        binding.messageTitleView.text = context.getString(R.string.suggested_edits_paused_title)
        binding.messageTextView.text = StringUtil.fromHtml(message)
        binding.imageView.setImageResource(R.drawable.ic_suggested_edits_paused)
    }

    fun setDisabled(message: String) {
        setDefaultState()
        binding.messageTitleView.text = context.getString(R.string.suggested_edits_disabled_title)
        binding.messageTextView.text = StringUtil.fromHtml(message)
        binding.imageView.setImageResource(R.drawable.ic_suggested_edits_disabled)
    }

    fun setIPBlocked(message: String? = null) {
        setDefaultState()
        binding.imageView.visibility = GONE
        if (message.isNullOrEmpty()) {
            binding.messageTitleView.visibility = VISIBLE
            binding.messageTitleView.text = context.getString(R.string.suggested_edits_ip_blocked_title)
            binding.messageTextView.text = context.getString(R.string.suggested_edits_ip_blocked_message)
        } else {
            binding.messageTitleView.visibility = GONE
            binding.messageTextView.text = StringUtil.fromHtml(message)
            binding.messageTextView.movementMethod = LinkMovementMethodExt.getExternalLinkMovementMethod()
            RichTextUtil.removeUnderlinesFromLinks(binding.messageTextView)
        }
        binding.positiveButton.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.create_account_ip_block_help_url))) }
        setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.create_account_ip_block_help_url))) }
    }

    fun setRequiredLogin(fragment: Fragment) {
        binding.imageView.visibility = View.VISIBLE
        binding.messageTitleView.text = context.getString(R.string.suggested_edits_encourage_account_creation_title)
        binding.messageTextView.text = context.getString(R.string.suggested_edits_encourage_account_creation_message)
        binding.imageView.setImageResource(R.drawable.ic_require_login_header)
        binding.positiveButton.text = context.getString(R.string.suggested_edits_encourage_account_creation_login_button)
        binding.positiveButton.setOnClickListener { fragment.startActivityForResult(LoginActivity.newIntent(context, SOURCE_SUGGESTED_EDITS), Constants.ACTIVITY_REQUEST_LOGIN) }
        binding.containerClickArea.setOnClickListener { fragment.startActivityForResult(LoginActivity.newIntent(context, SOURCE_SUGGESTED_EDITS), Constants.ACTIVITY_REQUEST_LOGIN) }
    }

    private fun setDefaultState() {
        binding.imageView.visibility = View.VISIBLE
        binding.positiveButton.text = context.getString(R.string.suggested_edits_learn_more)
        binding.positiveButton.setIconResource(R.drawable.ic_open_in_new_black_24px)
        binding.positiveButton.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.android_app_edit_help_url))) }
        binding.containerClickArea.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.android_app_edit_help_url))) }
    }
}
