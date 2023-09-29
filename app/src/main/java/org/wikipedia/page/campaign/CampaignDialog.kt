package org.wikipedia.page.campaign

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.donate.Campaign
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil

class CampaignDialog internal constructor(context: Context, val campaign: Campaign) : AlertDialog.Builder(context), CampaignDialogView.Callback {

    private var dialog: AlertDialog? = null

    init {
        val campaignView = CampaignDialogView(context)
        campaignView.campaignAssets = campaign.assets[WikipediaApp.instance.appOrSystemLanguageCode]
        campaignView.callback = this
        campaignView.setupViews()
        setView(campaignView)
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }

    private fun dismissDialog() {
        Prefs.announcementShownDialogs = setOf(campaign.id)
        dialog?.dismiss()
    }

    override fun onPositiveAction(url: String) {
        // TODO: maybe use the WebView after we figured out the way of perform the all JavaScript function in the donation page.
        // context.startActivity(SingleWebViewActivity.newIntent(context, newUrl))
        CustomTabsUtil.openInCustomTab(context, url)
        dismissDialog()
    }

    override fun onNegativeAction() {
        // TODO: implement negative action: I already donated
        dismissDialog()
    }

    override fun onNeutralAction() {
        // TODO: implement neutral action: maybe later
        dismissDialog()
    }
}
