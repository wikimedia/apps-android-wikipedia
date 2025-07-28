package org.wikipedia.donate.donationreminder

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StringRes
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

    fun setMessageTitle(title: String) {
        val titleWithReservedSpace = "$title    %" // HACK: Reserve space for the icon
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

    fun setMessageText(text: String) {
        binding.messageTextView.text = text
    }

    fun setPositiveButton(@StringRes stringRes: Int, listener: OnClickListener) {
        binding.positiveButton.text = context.getString(stringRes)
        binding.positiveButton.setOnClickListener(listener)
    }

    fun setNegativeButton(@StringRes stringRes: Int, listener: OnClickListener) {
        binding.negativeButton.text = context.getString(stringRes)
        binding.negativeButton.setOnClickListener(listener)
    }

    fun setMessageLabel(message: String?) {
        binding.messageLabel.text = message
        binding.messageLabel.typeface = Typeface.MONOSPACE
        binding.messageLabel.letterSpacing = 0.1f
    }
}
