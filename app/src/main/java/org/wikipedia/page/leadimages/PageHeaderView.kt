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
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.GradientUtil
import org.wikipedia.util.ResourceUtil
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
    var messageCardViewHeight: Int = 0
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

    fun show() {
        // First, make the donation card visible but keep the container hidden to measure
        binding.donationReminderCardView.visibility = VISIBLE
        visibility = INVISIBLE
        binding.donationReminderCardView.post {
            val widthSpec = MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, MeasureSpec.EXACTLY)
            val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

            binding.donationReminderCardView.measure(widthSpec, heightSpec)
            // Manually adjust the height of the message card view
            messageCardViewHeight = binding.donationReminderCardView.measuredHeight + DimenUtil.dpToPx(64f).toInt()

            layoutParams = CoordinatorLayout.LayoutParams(LayoutParams.MATCH_PARENT, DimenUtil.leadImageHeightForDevice(context) + messageCardViewHeight)
            visibility = VISIBLE
        }
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
        if (url.isNullOrEmpty()) {
            hide()
        } else {
            show()
            binding.viewPageHeaderImage.loadImage(url.toUri())
        }
    }

    fun setDonationReminderCard() {
        binding.donationReminderCardView.setMessageLabel(context.getString(R.string.recommended_reading_list_onboarding_card_new))
        binding.donationReminderCardView.setMessageTitle(context.getString(R.string.recommended_reading_list_onboarding_card_title))
        binding.donationReminderCardView.setMessageText(context.getString(R.string.recommended_reading_list_onboarding_card_message))
        binding.donationReminderCardView.setImageResource(-1, false)
        binding.donationReminderCardView.setPositiveButton(R.string.recommended_reading_list_onboarding_card_positive_button, {
            callback?.donationReminderCardPositiveClicked()
        }, false)
        binding.donationReminderCardView.setNegativeButton(R.string.recommended_reading_list_onboarding_card_negative_button, {
            callback?.donationReminderCardNegativeClicked()
            binding.donationReminderCardView.isVisible = false
        }, false)
        binding.donationReminderCardView.isVisible = true
    }
}
