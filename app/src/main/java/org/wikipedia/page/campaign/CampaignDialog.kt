package org.wikipedia.page.campaign

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.dataclient.donate.Campaign
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.FeedbackUtil
import java.time.*
import java.time.temporal.ChronoUnit

class CampaignDialog internal constructor(private val context: Context, val campaign: Campaign) : AlertDialog.Builder(context), CampaignDialogView.Callback {

    private var dialog: AlertDialog? = null

    init {
        val campaignView = CampaignDialogView(context)
        campaignView.campaignAssets = campaign.assets[WikipediaApp.instance.appOrSystemLanguageCode]
        campaignView.callback = this
        val announcementPause = LocalDateTime.ofInstant(Prefs.announcementPauseTime, ZoneId.systemDefault())
        val now = LocalDateTime.now()
        val dateDiff = announcementPause.until(now, ChronoUnit.DAYS)
        campaignView.showNeutralButton = dateDiff >= 1 && campaign.endTime?.isAfter(now.plusDays(1)) == true
        campaignView.setupViews()
        setView(campaignView)
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }

    private fun dismissDialog(skipCampaign: Boolean = true) {
        // "Maybe later" option will show up the campaign after one day.
        if (skipCampaign) {
            Prefs.announcementShownDialogs = setOf(campaign.id)
        }
        dialog?.dismiss()
    }

    override fun onPositiveAction(url: String) {
        DonorExperienceEvent.logAction("donate_start_click", "article_banner", campaignId = campaign.id)
        val customTabUrl = Prefs.announcementCustomTabTestUrl.orEmpty().ifEmpty { url }
        CustomTabsUtil.openInCustomTab(context, customTabUrl)
        dismissDialog()
    }

    override fun onNegativeAction() {
        DonorExperienceEvent.logAction("already_donated_click", "article_banner", campaignId = campaign.id)
        FeedbackUtil.showMessage(context as Activity, R.string.donation_campaign_donated_snackbar)
        dismissDialog()
    }

    override fun onNeutralAction() {
        DonorExperienceEvent.logAction("later_click", "article_banner", campaignId = campaign.id)
        Prefs.announcementPauseTime = Instant.now()
        FeedbackUtil.showMessage(context as Activity, R.string.donation_campaign_maybe_later_snackbar)
        DonorExperienceEvent.logAction("reminder_toast", "article_banner", campaignId = campaign.id)
        dismissDialog(false)
    }

    override fun onClose() {
        DonorExperienceEvent.logAction("close_click", "article_banner", campaignId = campaign.id)
        dismissDialog()
    }
}
