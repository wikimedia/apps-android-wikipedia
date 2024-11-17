package org.wikipedia.analytics.eventplatform

import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.donate.CampaignCollection
import org.wikipedia.settings.Prefs
import org.wikipedia.usercontrib.ContributionsDashboardHelper

class ContributionsDashboardEvent : DonorExperienceEvent() {

    companion object {

        fun logAction(
            action: String,
            activeInterface: String,
            wikiId: String = WikipediaApp.instance.appOrSystemLanguageCode,
            campaignId: String? = null
        ) {
            if (ContributionsDashboardHelper.contributionsDashboardEnabled) {
                submit(
                    action,
                    activeInterface,
                    campaignId?.let { "campaign_id: ${CampaignCollection.getFormattedCampaignId(it)}, " }
                        .orEmpty() + "donor_detected: ${Prefs.isDonor}",
                    wikiId
                )
            }
        }
    }
}
