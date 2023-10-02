package org.wikipedia.page.campaign

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.donate.Campaign
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.FeedbackUtil
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date

class CampaignDialog internal constructor(private val context: Context, val campaign: Campaign) : AlertDialog.Builder(context), CampaignDialogView.Callback {

    private var dialog: AlertDialog? = null

    init {
        val campaignView = CampaignDialogView(context)
        campaignView.campaignAssets = campaign.assets[WikipediaApp.instance.appOrSystemLanguageCode]
        campaignView.callback = this
        val dateDiff = Duration.between(Instant.ofEpochMilli(Prefs.announcementPauseTime), Instant.now())
        campaignView.showNeutralButton = dateDiff.toDays() >= 1 && campaign.endDateTime?.isAfter(LocalDateTime.now().plusDays(1)) == true
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
        // TODO: maybe use the WebView after we figured out the way of perform the all JavaScript function in the donation page.
        // context.startActivity(SingleWebViewActivity.newIntent(context, newUrl))
        CustomTabsUtil.openInCustomTab(context, url)
        dismissDialog()
    }

    override fun onNegativeAction() {
        FeedbackUtil.showMessage(context as Activity, R.string.donation_campaign_donated_snackbar)
        dismissDialog()
    }

    override fun onNeutralAction() {
        Prefs.announcementPauseTime = Date().time
        FeedbackUtil.showMessage(context as Activity, R.string.donation_campaign_maybe_later_snackbar)
        dismissDialog(false)
    }

    override fun onClose() {
        dismissDialog()
    }
}
