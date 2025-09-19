package org.wikipedia.settings

import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.recurring.RecurringTask
import java.util.Date
import java.util.concurrent.TimeUnit

class RemoteConfigRefreshTask() : RecurringTask() {
    override val name = "remote-config-refresher"

    override fun shouldRun(lastRun: Date): Boolean {
        return millisSinceLastRun(lastRun) >= TimeUnit.DAYS.toMillis(1)
    }

    override suspend fun run(lastRun: Date) {
        val config = ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getConfiguration()
        RemoteConfig.updateConfig(config)

        val userInfo = ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserInfo()
        // This clumsy comparison is necessary because the field is an integer value when enabled, but an empty string when disabled.
        // Since we want the default to lean towards opt-in, we check very specifically for an empty string, to make sure the user has opted out.
        val fundraisingOptOut = userInfo.query?.userInfo?.options?.fundraisingOptIn?.toString()?.replace("\"", "")?.isEmpty()
        Prefs.donationBannerOptIn = fundraisingOptOut != true
    }
}
