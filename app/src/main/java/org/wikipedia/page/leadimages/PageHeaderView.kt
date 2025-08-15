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
        fun donationReminderCardPositiveClicked(isInitialPrompt: Boolean) // TODO: remove after the experiment
        fun donationReminderCardNegativeClicked(isInitialPrompt: Boolean) // TODO: remove after the experiment
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
            val isInitialPrompt = DonationReminderHelper.maybeShowInitialDonationReminder(false)
            val labelText = if (isInitialPrompt) {
                context.getString(R.string.donation_reminders_initial_prompt_label)
            } else {
                null
            }
            val titleText = if (isInitialPrompt) {
                context.getString(R.string.donation_reminders_initial_prompt_title)
            } else {
                val articleText = context.resources.getQuantityString(
                    R.plurals.donation_reminders_text_articles, config.articleFrequency, config.articleFrequency
                )
                val donationAmount =
                    DonateUtil.currencyFormat.format(Prefs.donationReminderConfig.donateAmount)
                context.getString(R.string.donation_reminders_prompt_title, articleText, donationAmount)
            }
            val messageText = if (isInitialPrompt) {
                context.getString(R.string.donation_reminders_initial_prompt_message)
            } else {
                val dateText = DateUtil.getShortDateString(Date(config.setupTimestamp))
                val articleText = context.resources.getQuantityString(
                    R.plurals.donation_reminders_text_articles, config.articleFrequency, config.articleFrequency
                )
                val donationAmount =
                    DonateUtil.currencyFormat.format(Prefs.donationReminderConfig.donateAmount)
                context.getString(R.string.donation_reminders_prompt_message, dateText, articleText, donationAmount)
            }
            val positiveButtonText = if (isInitialPrompt) {
                context.getString(R.string.donation_reminders_initial_prompt_positive_button)
            } else {
                context.getString(R.string.donation_reminders_prompt_positive_button)
            }
            val negativeButtonText = if (isInitialPrompt) {
                context.getString(R.string.donation_reminders_initial_prompt_negative_button)
            } else {
                context.getString(R.string.donation_reminders_prompt_negative_button)
            }
            binding.donationReminderCardView.setLabel(labelText)
            binding.donationReminderCardView.setTitle(titleText)
            binding.donationReminderCardView.setMessage(messageText)
            binding.donationReminderCardView.setPositiveButton(positiveButtonText) {
                callback?.donationReminderCardPositiveClicked(isInitialPrompt)
                DonationReminderHelper.donationReminderDismissed(isInitialPrompt)
            }
            binding.donationReminderCardView.setNegativeButton(negativeButtonText) {
                callback?.donationReminderCardNegativeClicked(isInitialPrompt)
                binding.donationReminderCardView.isVisible = false
                if (!isInitialPrompt && Prefs.donationReminderConfig.finalPromptCount == DonationReminderHelper.MAX_REMINDER_PROMPTS) {
                    // Give the user one more chance to see the donation reminder
                    Prefs.donationReminderConfig = Prefs.donationReminderConfig.copy(
                        finalPromptCount = 1,
                        finalPromptActive = true
                    )
                    return@setNegativeButton
                }
                DonationReminderHelper.donationReminderDismissed(isInitialPrompt)
            }

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

    fun maybeShowDonationReminderCard() {
        if (!DonationReminderHelper.hasActiveReminder) {
            return
        }
        val canShowInitialDonationReminder = DonationReminderHelper.maybeShowInitialDonationReminder(true)
        val canShowFinalDonationReminder = DonationReminderHelper.maybeShowDonationReminder(true)
        if (canShowInitialDonationReminder) {
            DonorExperienceEvent.logDonationReminderAction(
                activeInterface = "reminder_start",
                action = "impression"
            )
        }

        if (canShowFinalDonationReminder) {
            DonorExperienceEvent.logDonationReminderAction(
                activeInterface = "reminder_milestone",
                action = "impression"
            )
        }
        binding.donationReminderCardView.isVisible = canShowInitialDonationReminder || canShowFinalDonationReminder
    }
}
