package org.wikipedia.donate.donationreminder

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.databinding.ViewDonationReminderCardBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.WikiCardView

class DonationReminderCardView(context: Context, attrs: AttributeSet? = null) : WikiCardView(context, attrs) {

    val binding = ViewDonationReminderCardBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        elevation = 0f
    }

    fun setTitle(title: String) {
        val titleWithReservedSpace = "$title  %" // HACK: Reserve space for the icon
        val spannableString = SpannableString(titleWithReservedSpace)
        val iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_heart_24)!!
        val iconSize = DimenUtil.dpToPx(24f).toInt()
        iconDrawable.apply {
            setTint(ResourceUtil.getThemedColor(context, R.attr.destructive_color))
            setBounds(0, 0, iconSize, iconSize)
        }
        spannableString.setSpan(ImageSpan(iconDrawable, ImageSpan.ALIGN_BOTTOM), titleWithReservedSpace.length - 1, titleWithReservedSpace.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.messageTitleView.text = spannableString
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

    fun setLabel(message: String?) {
        if (message.isNullOrEmpty()) {
            binding.messageLabel.visibility = GONE
            return
        }
        binding.messageLabel.text = message
        binding.messageLabel.typeface = Typeface.MONOSPACE
        binding.messageLabel.letterSpacing = 0.1f
    }
}
