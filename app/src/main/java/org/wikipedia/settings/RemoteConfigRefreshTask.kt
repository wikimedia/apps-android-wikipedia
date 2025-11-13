package org.wikipedia.settings

import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.recurring.RecurringTask
import java.util.Date
import java.util.concurrent.TimeUnit

class RemoteConfigRefreshTask() : RecurringTask() {
    override val name = "remote-config-refresher"

    override fun shouldRun(lastRun: Date): Boolean {
        return millisSinceLastRun(lastRun) >= TimeUnit.HOURS.toMillis(6)
    }

    override suspend fun run(lastRun: Date) {
        val config = ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getConfiguration()
        RemoteConfig.updateConfig(config)

        if (AccountUtil.isLoggedIn) {
            val userInfo = ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserInfo()
            Prefs.donationBannerOptIn = userInfo.query?.userInfo?.options?.optedInToFundraising == true
        }
    }
}
