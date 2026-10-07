package org.wikipedia.settings.dev.playground

import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewReadingStats
import org.wikipedia.yearinreview.data.YearInReviewRewardData
import org.wikipedia.yearinreview.data.YearInReviewSnapshot

enum class YearInReviewPlaygroundData(val label: String, val readingStats: YearInReviewReadingStats?) {
    REAL("Real data", null),
    LOW_DATA("Low data", YearInReviewReadingStats(articlesReadCount = 0, visitedDaysCount = 0, peakMonth = 0, peakMonthVisitedDays = 0)),
    DATA_RICH("Data rich", YearInReviewReadingStats(articlesReadCount = 120, visitedDaysCount = 45, peakMonth = 12, peakMonthVisitedDays = 20))
}

object YearInReviewPlayground {
    // null when real data is chosen, and always outside pre-production builds
    val snapshot: YearInReviewSnapshot? get() {
        val data = Prefs.yearInReviewPlaygroundData
        val remoteConfig = YearInReviewConfig.cachedRemoteConfig ?: YearInReviewConfig.fallbackConfig
        if (!ReleaseUtil.isPreProdRelease || data == YearInReviewPlaygroundData.REAL) {
            return null
        }
        // Login stays real, so the personalized flow still needs a logged-in user.
        return YearInReviewSnapshot(
            year = YearInReviewConfig.YEAR,
            isLoggedIn = AccountUtil.isLoggedIn,
            isDonationEligible = true,
            remoteConfig = remoteConfig,
            readingStats = data.readingStats ?: YearInReviewReadingStats(articlesReadCount = 0, visitedDaysCount = 0, peakMonth = 0, peakMonthVisitedDays = 0),
            rewardData = YearInReviewRewardData(isDonor = false, isEditor = false)
        )
    }
}
