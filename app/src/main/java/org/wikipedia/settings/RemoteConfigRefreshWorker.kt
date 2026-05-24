package org.wikipedia.settings

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory

class RemoteConfigRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val config = ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getConfiguration()
        RemoteConfig.updateConfig(config)

        if (AccountUtil.isLoggedIn) {
            val userInfo = ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserInfo()
            Prefs.donationBannerOptIn = userInfo.query?.userInfo?.options?.optedInToFundraising == true
        }

        return Result.success()
    }
}
