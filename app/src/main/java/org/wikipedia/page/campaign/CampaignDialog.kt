package org.wikipedia.page.campaign

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.dataclient.donate.Campaign
import org.wikipedia.donate.donationreminder.DonationReminderAbTest
import org.wikipedia.donate.donationreminder.DonationReminderHelper
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.FeedbackUtil
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date

class CampaignDialog internal constructor(private val context: Context, val campaign: Campaign, val onNeutralButtonClick: ((campaignId: String) -> Unit)? = null) : AlertDialog.Builder(context), CampaignDialogView.Callback {
    private var dialog: AlertDialog? = null
    private val campaignIdOriginal = campaign.getIdForLang(WikipediaApp.instance.appOrSystemLanguageCode)
    private val campaignId = campaignIdOriginal + if (DonationReminderHelper.isInEligibleCountry) {
        if (DonationReminderAbTest().isTestGroupUser()) "_reminderB" else "_reminderA"
    } else ""

    init {
        val campaignView = CampaignDialogView(context)
        campaignView.callback = this
        val dateDiff = Duration.between(Instant.ofEpochMilli(Prefs.announcementPauseTime), Instant.now())
        campaignView.showNeutralButton = dateDiff.toDays() >= 1 && campaign.endDateTime?.isAfter(LocalDateTime.now().plusDays(1)) == true || Prefs.ignoreDateForAnnouncements
        campaignView.setupViews(campaignId, campaign.getAssetsForLang(WikipediaApp.instance.appOrSystemLanguageCode))
        setView(campaignView)

        DonorExperienceEvent.logAction("impression", "article_banner", campaignId = campaignId)
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    private fun dismissDialog(skipCampaign: Boolean = true) {
        // "Maybe later" option will show up the campaign after one day.
        if (skipCampaign) {
            Prefs.announcementShownDialogs = setOf(campaignIdOriginal)
        }
        dialog?.dismiss()
    }

    override fun onPositiveAction(url: String) {
        DonorExperienceEvent.logAction("donate_start_click", "article_banner", campaignId = campaignId)
        val customTabUrl = Prefs.announcementCustomTabTestUrl.orEmpty().ifEmpty { url }
        if (context is BaseActivity) {
            context.launchDonateDialog(campaignId, customTabUrl)
            dismissDialog(false)
        } else {
            CustomTabsUtil.openInCustomTab(context, customTabUrl)
            dismissDialog()
        }
    }

    override fun onNegativeAction() {
        DonorExperienceEvent.logAction("already_donated_click", "article_banner", campaignId = campaignId)
        FeedbackUtil.showMessage(context as Activity, R.string.donation_campaign_donated_snackbar)
        dismissDialog()
    }

    override fun onNeutralAction() {
        DonorExperienceEvent.logAction("later_click", "article_banner", campaignId = campaignId)
        DonorExperienceEvent.logAction("reminder_toast", "article_banner", campaignId = campaignId)
        if (!DonationReminderHelper.isEnabled) {
            Prefs.announcementPauseTime = Date().time
            FeedbackUtil.showMessage(context as Activity, R.string.donation_campaign_maybe_later_snackbar)
            dismissDialog(false)
            return
        }
        Prefs.announcementShownDialogs = setOf(campaignIdOriginal)
        onNeutralButtonClick?.invoke(campaignId)
    }

    override fun onClose() {
        DonorExperienceEvent.logAction("close_click", "article_banner", campaignId = campaignId)
        dismissDialog()
    }
}
