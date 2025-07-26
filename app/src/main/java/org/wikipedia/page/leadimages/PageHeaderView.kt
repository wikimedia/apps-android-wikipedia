package org.wikipedia.page.leadimages

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ViewPageHeaderBinding
import org.wikipedia.donate.donationreminder.DonationReminderHelper
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.GradientUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.LinearLayoutOverWebView
import org.wikipedia.views.ObservableWebView

class PageHeaderView(context: Context, attrs: AttributeSet? = null) : LinearLayoutOverWebView(context, attrs), ObservableWebView.OnScrollChangeListener {
    interface Callback {
        fun onImageClicked()
        fun onCallToActionClicked()
        fun donationReminderCardPositiveClicked()
        fun donationReminderCardNegativeClicked()
    }

    private val binding = ViewPageHeaderBinding.inflate(LayoutInflater.from(context), this)
    private var messageCardViewHeight: Int = 0
    val donationReminderCardViewHeight get() = if (binding.donationReminderCardView.isVisible) messageCardViewHeight else 0
    var callToActionText: String? = null
        set(value) {
            field = value
            refreshCallToActionVisibility()
        }

    var callback: Callback? = null
    val imageView get() = binding.viewPageHeaderImage

    init {
        binding.viewPageHeaderImageGradientBottom.background = GradientUtil.getPowerGradient(ResourceUtil.getThemedColor(context, R.attr.overlay_color), Gravity.BOTTOM)
        binding.viewPageHeaderImage.setOnClickListener {
            callback?.onImageClicked()
        }
        binding.callToActionContainer.setOnClickListener {
            callback?.onCallToActionClicked()
        }
        setDonationReminderCard()
        orientation = VERTICAL
    }

    override fun onScrollChanged(oldScrollY: Int, scrollY: Int, isHumanScroll: Boolean) {
        updateScroll(scrollY)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateScroll()
    }

    private fun updateScroll(scrollY: Int = -translationY.toInt()) {
        binding.viewPageHeaderImage.translationY = 0f
        translationY = -height.coerceAtMost(scrollY).toFloat()
    }

    fun hide() {
        visibility = GONE
    }

    fun hideImage() {
        binding.headerImageContainer.isVisible = false
        layoutParams = CoordinatorLayout.LayoutParams(LayoutParams.MATCH_PARENT, donationReminderCardViewHeight)
        // TODO: fix the bottom space when no image is shown
        visibility = VISIBLE
    }

    fun show() {
        layoutParams = CoordinatorLayout.LayoutParams(LayoutParams.MATCH_PARENT, DimenUtil.leadImageHeightForDevice(context) + donationReminderCardViewHeight)
        visibility = VISIBLE
    }

    fun showImage() {
        binding.headerImageContainer.isVisible = true
        show()
    }

    fun refreshCallToActionVisibility() {
        if (callToActionText != null && !Prefs.readingFocusModeEnabled) {
            binding.callToActionContainer.visibility = VISIBLE
            binding.callToActionText.text = callToActionText
            binding.viewPageHeaderImageGradientBottom.visibility = VISIBLE
        } else {
            binding.callToActionContainer.visibility = GONE
            binding.viewPageHeaderImageGradientBottom.visibility = GONE
        }
    }

    fun loadImage(url: String?) {
        maybeShowDonationReminderCard()
        if (url.isNullOrEmpty()) {
            hideImage()
        } else {
            showImage()
            binding.viewPageHeaderImage.loadImage(url.toUri())
        }
        L.d("PageHeaderView: loadImage: ${layoutParams.height} => $donationReminderCardViewHeight")
    }

    private fun setDonationReminderCard() {
        // TODO: setup the text based on the donation reminder settings
        // TODO: make sure to set up the different actions for different cards (and update preferences too)
        binding.donationReminderCardView.setMessageLabel(context.getString(R.string.donation_reminder_initial_prompt_label))
        binding.donationReminderCardView.setMessageTitle(context.getString(R.string.donation_reminder_initial_prompt_title))
        binding.donationReminderCardView.setMessageText(context.getString(R.string.donation_reminder_initial_prompt_message))
        binding.donationReminderCardView.setPositiveButton(R.string.donation_reminder_initial_prompt_positive_button) {
            callback?.donationReminderCardPositiveClicked()
        }
        binding.donationReminderCardView.setNegativeButton(R.string.donation_reminder_initial_prompt_negative_button) {
            callback?.donationReminderCardNegativeClicked()
            binding.donationReminderCardView.isVisible = false
        }

        binding.donationReminderCardView.isVisible = true
        visibility = INVISIBLE
        binding.donationReminderCardView.post {
            val widthSpec = MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

            binding.donationReminderCardView.measure(widthSpec, heightSpec)
            // Manually adjust the height of the message card view
            messageCardViewHeight = binding.donationReminderCardView.measuredHeight + DimenUtil.dpToPx(64f).toInt()
            binding.donationReminderCardView.isVisible = false
            visibility = GONE
        }
    }

    fun maybeShowDonationReminderCard() {
        if (!DonationReminderHelper.maybeShowInitialDonationReminder(false)) {
            return
        }
        binding.donationReminderCardView.isVisible = true
    }
}
