package org.wikipedia.donate.donationreminder

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import org.wikipedia.databinding.ViewDonationReminderCardBinding
import org.wikipedia.util.DimenUtil
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
        binding.messageTextView.text = text
    }

    fun setPositiveButton(text: String, listener: OnClickListener) {
        binding.positiveButton.text = text
        binding.positiveButton.setOnClickListener(listener)
    }

    fun setNegativeButton(text: String, listener: OnClickListener) {
        binding.negativeButton.text = text
        binding.negativeButton.setOnClickListener(listener)
    }
}
