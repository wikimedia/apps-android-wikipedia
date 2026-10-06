package org.wikipedia.yearinreview.data

import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.db.HistoryEntryDao
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.log.L
import java.io.IOException

interface YearInReviewRepository {
    suspend fun getYearInReview(): YearInReviewSnapshot
}

class YearInReviewRepositoryImpl(
    private val restService: RestService = ServiceFactory.getRest(WikipediaApp.instance.wikiSite),
    private val historyEntryDao: HistoryEntryDao = AppDatabase.instance.historyEntryDao(),
    private val cache: YearInReviewCache = PrefsYearInReviewStore,
    private val donationEligibility: YearInReviewDonationEligibility = YearInReviewDonationEligibility()
) : YearInReviewRepository {

    override suspend fun getYearInReview(): YearInReviewSnapshot {
        val year = YearInReviewConfig.YEAR
        val remoteConfig = restService.getConfiguration().commonv1?.getYirForYear(year)
        val isDonationEligible = remoteConfig != null && !remoteConfig.hideDonateCountryCodes.contains(GeoUtil.geoIPCountry.orEmpty())

        val cachedStats = cache.get(year)
        val readingStats = cachedStats?.readingStats ?: getReadingStats(YearInReviewConfig.insightsDateRange(remoteConfig))
        val editingStats = cachedStats?.editingStats ?: getContributorEditingStats(YearInReviewConfig.contributionsDateRange(remoteConfig))
        val updatedStats = YearInReviewCachedStats(readingStats = readingStats, editingStats = editingStats)
        if (updatedStats != cachedStats) {
            cache.put(year, updatedStats)
        }
        if (cachedStats == null) {
            Prefs.yearInReviewSurveyState = YearInReviewSurveyState.NOT_TRIGGERED
        }

        return YearInReviewSnapshot(
            year = year,
            isLoggedIn = AccountUtil.isLoggedIn,
            isDonationEligible = isDonationEligible,
            remoteConfig = remoteConfig,
            readingStats = readingStats,
            editingStats = editingStats,
            rewardData = YearInReviewRewardData(
                isDonor = donationEligibility.hasDonatedWithinContributionsDateRange(remoteConfig),
                isEditor = (editingStats?.contributionsEditCount ?: 0) > 0
            )
        )
    }

    private suspend fun getReadingStats(dateRange: YearInReviewDateRange): YearInReviewReadingStats {
        val peakMonth = historyEntryDao.getPeakMonthByVisitedDaysBetween(dateRange.startMillis, dateRange.endMillis)
        return YearInReviewReadingStats(
            articlesReadCount = historyEntryDao.getDistinctEntriesCountBetween(dateRange.startMillis, dateRange.endMillis),
            visitedDaysCount = historyEntryDao.getDistinctDaysCountBetween(dateRange.startMillis, dateRange.endMillis),
            peakMonth = peakMonth?.month ?: 0,
            peakMonthVisitedDays = peakMonth?.visitedDays ?: 0
        )
    }

    // Returns null when logged out, or when the request fails so that it's retried next time instead of caching zero edits.
    private suspend fun getContributorEditingStats(dateRange: YearInReviewDateRange): YearInReviewEditingStats? {
        if (!AccountUtil.isLoggedIn) {
            return null
        }
        return try {
            val userInfoResponse = ServiceFactory.get(WikipediaApp.instance.wikiSite).getLocalAndGlobalUserInfo()
            val globalUserId = userInfoResponse.query?.globalUserInfo?.id ?: return null
            val edits = ServiceFactory.getRest(WikiSite(Service.WIKIMEDIA_URL))
                .getEditsPerGlobalUserMonthly(
                    globalUserId,
                    DateUtil.getYMDDateString(dateRange.start),
                    DateUtil.getYMDDateString(dateRange.endInclusive)
                )
            YearInReviewEditingStats(contributionsEditCount = edits.items.sumOf { it.editCount })
        } catch (e: IOException) {
            L.e(e)
            null
        }
    }
}
