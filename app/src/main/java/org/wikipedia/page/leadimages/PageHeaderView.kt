package org.wikipedia.page.leadimages

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.databinding.ViewPageHeaderBinding
import org.wikipedia.donate.DonateUtil
import org.wikipedia.donate.donationreminder.DonationReminderConfig
import org.wikipedia.donate.donationreminder.DonationReminderHelper
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.GradientUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.LinearLayoutOverWebView
import org.wikipedia.views.ObservableWebView
import java.util.Date

class PageHeaderView(context: Context, attrs: AttributeSet? = null) : LinearLayoutOverWebView(context, attrs), ObservableWebView.OnScrollChangeListener {
    interface Callback {
        fun onImageClicked()
        fun onCallToActionClicked()
        fun donationReminderCardPositiveClicked()
        fun donationReminderCardNegativeClicked()
    }

    private val binding = ViewPageHeaderBinding.inflate(LayoutInflater.from(context), this)
    private var messageCardViewHeight: Int = 0
    val donationReminderCardViewHeight get() = if (binding.donationReminderCardView.isVisible) {
        // HACK: adjust the height for the message card to handle image/no image scenarios to make sure have better margins
        messageCardViewHeight + if (binding.headerImageContainer.isVisible) 0 else DimenUtil.dpToPx(20f).toInt()
    } else 0
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
        visibility = VISIBLE
    }

    fun hideDonationReminderCard() {
        binding.donationReminderCardView.isVisible = false
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
    }

    // TODO: remove after the experiment
    private fun setDonationReminderCard() {
        if (!DonationReminderHelper.isEnabled && !DonationReminderHelper.hasActiveReminder) {
            return
        }
        Prefs.donationReminderConfig.let { config ->
            updateDonationReminderCardContent(config)
            binding.donationReminderCardView.isVisible = true
            visibility = INVISIBLE
            binding.donationReminderCardView.post {
                val widthSpec = MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, MeasureSpec.EXACTLY)
                val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

                binding.donationReminderCardView.measure(widthSpec, heightSpec)
                // HACK: Manually adjust the height of the message card view
                messageCardViewHeight = binding.donationReminderCardView.measuredHeight + DimenUtil.dpToPx(64f).toInt()
                binding.donationReminderCardView.isVisible = false
                visibility = GONE
            }
        }
    }

    private fun updateDonationReminderCardContent(config: DonationReminderConfig?) {
        config?.let { config ->
            val articleText = context.resources.getQuantityString(
                R.plurals.donation_reminders_text_articles, config.articleFrequency, config.articleFrequency
            )
            val donationAmount = DonateUtil.currencyFormat.format(Prefs.donationReminderConfig.donateAmount)
            val titleText = if (config.goalReachedCount == 1) {
                context.getString(R.string.donation_reminders_first_milestone_reached_prompt_title, articleText, donationAmount)
            } else {
                context.getString(R.string.donation_reminders_subsequent_milestone_reached_prompt_title, articleText)
            }

            val dateText = DateUtil.getDateString(Date(config.setupTimestamp))
            val messageText = context.getString(R.string.donation_reminders_prompt_message, dateText, articleText, donationAmount)
            val positiveButtonText = context.getString(R.string.donation_reminders_prompt_positive_button)
            val negativeButtonText = context.getString(R.string.donation_reminders_prompt_negative_button)
            binding.donationReminderCardView.setTitle(titleText)
            binding.donationReminderCardView.setMessage(messageText)
            binding.donationReminderCardView.setPositiveButton(positiveButtonText) {
                callback?.donationReminderCardPositiveClicked()
                DonationReminderHelper.dismissReminder()
            }
            binding.donationReminderCardView.setNegativeButton(negativeButtonText) {
                callback?.donationReminderCardNegativeClicked()
                binding.donationReminderCardView.isVisible = false
                DonationReminderHelper.dismissReminder()
            }
        }
    }

    fun maybeShowDonationReminderCard() {
        if (DonationReminderHelper.shouldShowReminderNow()) {
            DonorExperienceEvent.logDonationReminderAction(
                activeInterface = "reminder_milestone",
                action = "impression"
            )
            updateDonationReminderCardContent(Prefs.donationReminderConfig)
            binding.donationReminderCardView.isVisible = true
        } else {
            binding.donationReminderCardView.isVisible = false
        }
    }
}
