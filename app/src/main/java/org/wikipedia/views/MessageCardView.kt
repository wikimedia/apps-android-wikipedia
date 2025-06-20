package org.wikipedia.views

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wikipedia.R
import org.wikipedia.databinding.ViewMessageCardBinding
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

class MessageCardView(context: Context, attrs: AttributeSet? = null) : WikiCardView(context, attrs) {

    val binding = ViewMessageCardBinding.inflate(LayoutInflater.from(context), this, true)

    fun setMessageTitle(title: String) {
        binding.messageTitleView.text = title
    }

    fun setMessageText(text: String) {
        binding.messageTextView.text = text
    }

    fun setImageResource(@DrawableRes imageResource: Int = -1, visible: Boolean) {
        if (visible) {
            binding.imageView.visibility = VISIBLE
            binding.imageView.setImageResource(imageResource)
        } else {
            binding.imageView.visibility = GONE
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
        binding.negativeButton.visibility = VISIBLE
        if (applyListenerToContainer) {
            binding.containerClickArea.setOnClickListener(listener)
        }
    }

    fun setOnboarding(message: String) {
        setImageResource(R.drawable.ic_suggested_edits_onboarding, true)
        binding.messageTitleView.visibility = GONE
        binding.messageTextView.text = StringUtil.fromHtml(message.toString())
        binding.buttonsContainer.visibility = GONE
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
        }
        binding.positiveButton.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.create_account_ip_block_help_url))) }
        setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.create_account_ip_block_help_url))) }
    }

    fun setRequiredLogin(onClickListener: OnClickListener) {
        binding.imageView.visibility = VISIBLE
        binding.messageTitleView.visibility = VISIBLE
        binding.buttonsContainer.visibility = VISIBLE
        binding.messageTitleView.text = context.getString(R.string.suggested_edits_encourage_account_creation_title)
        binding.messageTextView.text = context.getString(R.string.suggested_edits_encourage_account_creation_message)
        binding.imageView.setImageResource(R.drawable.ic_require_login_header)
        binding.positiveButton.text = context.getString(R.string.suggested_edits_encourage_account_creation_login_button)
        binding.positiveButton.setOnClickListener(onClickListener)
        binding.containerClickArea.setOnClickListener(onClickListener)
    }

    fun setMessageLabel(message: String?) {
        binding.messageLabel.text = message
        binding.messageLabel.visibility = if (message.isNullOrEmpty()) GONE else VISIBLE
    }

    private fun setDefaultState() {
        binding.imageView.visibility = VISIBLE
        binding.messageTitleView.visibility = VISIBLE
        binding.buttonsContainer.visibility = VISIBLE
        binding.positiveButton.text = context.getString(R.string.suggested_edits_learn_more)
        binding.positiveButton.setIconResource(R.drawable.ic_open_in_new_black_24px)
        binding.positiveButton.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.android_app_edit_help_url))) }
        binding.containerClickArea.setOnClickListener {
            UriUtil.visitInExternalBrowser(
                context,
                Uri.parse(context.getString(R.string.android_app_edit_help_url))
            )
        }
    }
}
