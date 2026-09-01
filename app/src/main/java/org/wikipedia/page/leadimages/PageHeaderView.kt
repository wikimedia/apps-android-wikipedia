package org.wikipedia.page.leadimages

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.databinding.ViewPageHeaderBinding
import org.wikipedia.donate.DonateUtil
import org.wikipedia.donate.donationreminder.DonationReminderAbTest
import org.wikipedia.donate.donationreminder.DonationReminderConfig
import org.wikipedia.donate.donationreminder.DonationReminderHelper
import org.wikipedia.donate.donationreminder.DonationReminderType
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
        fun donationReminderCardPositiveClicked(type: DonationReminderType)
        fun donationReminderCardNegativeClicked(type: DonationReminderType)
    }

    private val binding = ViewPageHeaderBinding.inflate(LayoutInflater.from(context), this)

    val donationReminderCardViewHeight get() = if (binding.donationReminderCardView.isVisible) {
        measureDonationReminderCardExtraHeight()
    } else 0

    private fun measureDonationReminderCardExtraHeight(): Int {
        val hasImage = binding.headerImageContainer.isVisible
        val originalHeight = binding.headerImageContainer.layoutParams.height
        if (hasImage) {
            binding.headerImageContainer.updateLayoutParams<LayoutParams> { height = 0 }
        }
        val widthSpec = MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, MeasureSpec.EXACTLY)
        val heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(widthSpec, heightSpec)
        val extraHeight = measuredHeight
        if (hasImage) {
            binding.headerImageContainer.updateLayoutParams<LayoutParams> { height = originalHeight }
        }
        return extraHeight
    }

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
        val leadImageHeight = DimenUtil.leadImageHeightForDevice(context)
        layoutParams = CoordinatorLayout.LayoutParams(LayoutParams.MATCH_PARENT, leadImageHeight + donationReminderCardViewHeight)
        binding.headerImageContainer.updateLayoutParams<LayoutParams> {
            height = leadImageHeight
            weight = 0f
        }
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

    private fun updateDonationReminderCardContent(config: DonationReminderConfig?) {
        config?.let { config ->
            val isWrapUpEnabled = DonationReminderHelper.isWrapUpEnabled
            val reminderCardType = if (isWrapUpEnabled) DonationReminderType.WRAP_UP else DonationReminderType.GENERAL
            val articleText = context.resources.getQuantityString(
                R.plurals.donation_reminders_text_articles, config.articleFrequency, config.articleFrequency
            )
            val donateAmount = if (Prefs.donationReminderConfig.donateAmount <= 0) {
                DonationReminderHelper.defaultDonateAmountOptions.first()
            } else {
                Prefs.donationReminderConfig.donateAmount
            }
            val donationAmountText = DonateUtil.currencyFormat.format(donateAmount)
            val titleText = if (isWrapUpEnabled) {
                if (DonationReminderAbTest().group == 1) {
                    context.getString(R.string.donation_reminders_wrap_up_title)
                } else {
                    context.getString(R.string.donation_reminders_eoe_title)
                }
            } else {
                if (config.goalReachedCount == 1) {
                    context.getString(R.string.donation_reminders_first_milestone_reached_prompt_title, articleText, donationAmountText)
                } else {
                    context.getString(R.string.donation_reminders_subsequent_milestone_reached_prompt_title, articleText)
                }
            }

            val dateText = DateUtil.getMMMMdYYYY(Date(config.setupTimestamp))
            val messageText = if (isWrapUpEnabled) {
                if (DonationReminderAbTest().group == 1) {
                    context.getString(R.string.donation_reminders_wrap_up_message)
                } else {
                    context.getString(R.string.donation_reminders_eoe_message, donationAmountText)
                }
            } else {
                context.getString(R.string.donation_reminders_prompt_message_v2, dateText, articleText, donationAmountText)
            }
            val positiveButtonText = if (isWrapUpEnabled) {
                if (DonationReminderAbTest().group == 1) {
                    context.getString(R.string.donation_reminders_wrap_up_share_feedback_button)
                } else {
                    context.getString(R.string.donation_reminders_eoe_give_monthly_button)
                }
            } else {
                context.getString(R.string.donation_reminders_prompt_positive_button_v2)
            }
            val negativeButtonText = if (isWrapUpEnabled) {
                context.getString(R.string.donation_reminders_settings_no_thanks_btn_label)
            } else {
                context.getString(R.string.donation_reminders_prompt_negative_button)
            }
            if (isWrapUpEnabled) {
                binding.donationReminderCardView.showWrapUpContainer()
            }
            binding.donationReminderCardView.setTitle(titleText)
            binding.donationReminderCardView.setMessage(messageText)
            binding.donationReminderCardView.setPositiveButton(positiveButtonText) {
                DonationReminderHelper.dismissReminder()
                callback?.donationReminderCardPositiveClicked(reminderCardType)
            }
            binding.donationReminderCardView.setNegativeButton(negativeButtonText) {
                binding.donationReminderCardView.isVisible = false
                DonationReminderHelper.dismissReminder()
                callback?.donationReminderCardNegativeClicked(reminderCardType)
            }
        }
    }

    fun maybeShowDonationReminderCard() {
        if (DonationReminderHelper.shouldShowReminderNow() || DonationReminderHelper.isWrapUpEnabled) {
            if (!DonationReminderHelper.isWrapUpEnabled) {
                DonorExperienceEvent.logDonationReminderAction(
                    activeInterface = "reminder_milestone",
                    action = "impression"
                )
            }
            updateDonationReminderCardContent(Prefs.donationReminderConfig)
            binding.donationReminderCardView.isVisible = true
        } else {
            binding.donationReminderCardView.isVisible = false
        }
    }
}
