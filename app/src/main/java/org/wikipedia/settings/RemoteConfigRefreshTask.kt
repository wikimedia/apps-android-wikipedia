package org.wikipedia.settings

import kotlinx.coroutines.CancellationException
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.recurring.RecurringTask
import org.wikipedia.util.log.L
import java.util.Date
import java.util.concurrent.TimeUnit

class RemoteConfigRefreshTask() : RecurringTask() {
    override val name = "remote-config-refresher"

    override fun shouldRun(lastRun: Date): Boolean {
        return Prefs.remoteConfigAppVersionCode < BuildConfig.VERSION_CODE ||
                millisSinceLastRun(lastRun) >= TimeUnit.HOURS.toMillis(6)
    }

    override suspend fun run(lastRun: Date) {
        val config = ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getConfiguration()
        RemoteConfig.updateConfig(config)
        Prefs.remoteConfigAppVersionCode = BuildConfig.VERSION_CODE

        if (AccountUtil.isLoggedIn) {
            val userInfo = ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserInfo()
            Prefs.donationBannerOptIn = userInfo.query?.userInfo?.options?.optedInToFundraising == true
        }
    }

    companion object {
        suspend fun refreshFromBackground() {
            try {
                RemoteConfigRefreshTask().runIfNecessary(force = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                L.w(e)
            }
        }
    }
}
