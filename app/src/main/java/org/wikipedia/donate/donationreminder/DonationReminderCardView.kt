package org.wikipedia.donate.donationreminder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.net.toUri
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ViewDonationReminderCardBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.WikiCardView

class DonationReminderCardView(context: Context, attrs: AttributeSet? = null) : WikiCardView(context, attrs) {

    val binding = ViewDonationReminderCardBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        strokeWidth = DimenUtil.roundedDpToPx(1f)
        elevation = 0f
    }

    fun setTitle(title: String) {
        binding.messageTitleView.text = title
    }

    fun setMessage(text: String) {
        binding.messageTextView.text = StringUtil.fromHtml(text)
    }

    fun setPositiveButton(text: String, listener: OnClickListener) {
        binding.positiveButton.text = text
        binding.positiveButton.setOnClickListener(listener)
    }

    fun setNegativeButton(text: String, listener: OnClickListener) {
        binding.negativeButton.text = text
        binding.negativeButton.setOnClickListener(listener)
    }

    fun showWrapUpContainer() {
        binding.wrapUpContainer.isVisible = true
        binding.learnMoreButton.setOnClickListener {
            UriUtil.visitInExternalBrowser(context, context.getString(R.string.donation_reminders_experiment_url).toUri())
        }
    }
}

enum class DonationReminderType {
    GENERAL, WRAP_UP
}
