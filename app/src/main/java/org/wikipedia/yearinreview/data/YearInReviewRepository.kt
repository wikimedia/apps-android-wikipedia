package org.wikipedia.yearinreview.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.restbase.PageViews
import org.wikipedia.history.db.HistoryEntryDao
import org.wikipedia.history.db.HistoryEntryWithImageDao
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.topics.db.PageTopicDao
import org.wikipedia.util.DateUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import java.io.IOException

interface YearInReviewRepository {
    suspend fun getYearInReview(): YearInReviewSnapshot
    suspend fun getEntryStats(): YearInReviewEntryStats
}

class YearInReviewRepositoryImpl(
    private val restService: RestService = ServiceFactory.getRest(WikipediaApp.instance.wikiSite),
    private val historyEntryDao: HistoryEntryDao = AppDatabase.instance.historyEntryDao(),
    private val historyEntryWithImageDao: HistoryEntryWithImageDao = AppDatabase.instance.historyEntryWithImageDao(),
    private val pageTopicDao: PageTopicDao = AppDatabase.instance.pageTopicDao(),
    private val cache: YearInReviewCache = PrefsYearInReviewStore,
    private val donationEligibility: YearInReviewDonationEligibility = YearInReviewDonationEligibility()
) : YearInReviewRepository {

    override suspend fun getYearInReview(): YearInReviewSnapshot {
        val year = YearInReviewConfig.YEAR
        val remoteConfig = restService.getConfiguration().commonv1?.getYirForYear(year) ?: YearInReviewConfig.fallbackConfig.also {
            L.e("Using fallback remote configuration!")
        }
        val isDonationEligible = !remoteConfig.hideDonateCountryCodes.contains(GeoUtil.geoIPCountry.orEmpty())

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
                isEditor = (editingStats?.totalEditCount ?: 0) > 0
            )
        )
    }

    // Only reads local data, so the entry card and announcement never wait on the network. Once Year in Review has been
    // calculated, its cached stats are used instead, so these counts match the ones the slides show.
    override suspend fun getEntryStats(): YearInReviewEntryStats {
        cache.get(YearInReviewConfig.YEAR)?.readingStats?.let {
            return YearInReviewEntryStats(articlesReadCount = it.articlesReadCount, visitedDaysCount = it.visitedDaysCount)
        }
        val dateRange = YearInReviewConfig.insightsDateRange(YearInReviewConfig.cachedRemoteConfig)
        return YearInReviewEntryStats(
            articlesReadCount = historyEntryDao.getDistinctArticlesCountBetween(dateRange.startMillis, dateRange.endMillis, mainPageTitles()),
            visitedDaysCount = historyEntryDao.getDistinctDaysCountBetween(dateRange.startMillis, dateRange.endMillis)
        )
    }

    suspend fun getReadingStats(dateRange: YearInReviewDateRange): YearInReviewReadingStats = coroutineScope {
        val mainPageTitles = mainPageTitles()

        val articlesReadCount = async { historyEntryDao.getDistinctArticlesCountBetween(dateRange.startMillis, dateRange.endMillis, mainPageTitles) }
        val visitedDaysCount = async { historyEntryDao.getDistinctDaysCountBetween(dateRange.startMillis, dateRange.endMillis) }
        val peakMonthDeferred = async { historyEntryDao.getPeakMonthByVisitedDaysBetween(dateRange.startMillis, dateRange.endMillis) }
        val articlesRevisited = async {
            historyEntryWithImageDao.findEntriesVisitedMultipleTimesBetween(dateRange.startMillis, dateRange.endMillis, mainPageTitles, limit = 3)
                .map { it.first.title to it.second }
        }

        // The sample articles query depends on the top topic, so the topic queries run in sequence here,
        // while the queries above keep running in parallel.
        val topTopics = pageTopicDao.getTopTopicsByArticleCount(dateRange.startMillis, dateRange.endMillis).take(4)
        val topTopicWithCount = topTopics.firstOrNull { it.articleCount >= 3 }
        val topTopic = topTopicWithCount?.topic?.let {
            ArticleTopics.all.find { topic -> topic.taxonIds.contains(it) }
        }
        val topTopicSampleArticles = topTopic?.let { topic ->
            pageTopicDao.findEntriesByTopic(topic.taxonIds.firstOrNull().orEmpty(), dateRange.startMillis, dateRange.endMillis, limit = 3)
                .map { StringUtil.fromHtml(it.displayTitle).toString() }
        }
        val otherTopTopics = topTopics.drop(1).filter { it.articleCount > 1 }

        val peakMonth = peakMonthDeferred.await()
        YearInReviewReadingStats(
            articlesReadCount = articlesReadCount.await(),
            visitedDaysCount = visitedDaysCount.await(),
            peakMonth = peakMonth?.month ?: 0,
            peakMonthVisitedDays = peakMonth?.visitedDays ?: 0,
            articlesRevisited = articlesRevisited.await(),
            topTopic = topTopic,
            topTopicTotalArticles = topTopicWithCount?.articleCount ?: 0,
            topTopicSampleArticles = topTopicSampleArticles,
            topicArticleCountMap = otherTopTopics.associate { entry ->
                val topic = ArticleTopics.all.find { topic -> topic.taxonIds.contains(entry.topic) }
                topic to entry.articleCount
            }.mapNotNull {
                if (it.key != null) it.key!! to it.value else null
            }.associate { it.first.topicId to it.second }
        )
    }

    private fun mainPageTitles(): List<String> {
        return WikipediaApp.instance.languageState.appLanguageCodes.map { StringUtil.addUnderscores(MainPageNameData.valueFor(it)) }
    }

    // Returns null when logged out, or when the request fails so that it's retried next time instead of caching zero edits.
    private suspend fun getContributorEditingStats(dateRange: YearInReviewDateRange): YearInReviewEditingStats? = coroutineScope {
        if (!AccountUtil.isLoggedIn) {
            return@coroutineScope null
        }
        try {
            val mainPageTitles = mainPageTitles()
            val userInfoResponse = ServiceFactory.get(WikipediaApp.instance.wikiSite).getLocalAndGlobalUserInfo()
            val globalUserId = userInfoResponse.query?.globalUserInfo?.id ?: return@coroutineScope null
            val totalEditsDeferred = async {
                ServiceFactory.getRest(WikiSite(Service.WIKIMEDIA_URL))
                    .getEditsPerGlobalUserMonthly(
                        globalUserId,
                        DateUtil.getYMDDateString(dateRange.start),
                        DateUtil.getYMDDateString(dateRange.endInclusive)
                    )
            }
            val totalPageViewsDeferred = async {
                ServiceFactory.getRest(WikiSite(Service.WIKIMEDIA_URL))
                    .getPageViewsForEditedPagesMonthly(
                        globalUserId,
                        DateUtil.getYMDDateString(dateRange.start),
                        DateUtil.getYMDDateString(dateRange.endInclusive)
                    )
            }
            val mostViewedArticlesDeferred = async {
                ServiceFactory.getRest(WikiSite(Service.WIKIMEDIA_URL))
                    .getMostViewedEditedPagesMonthly(
                        globalUserId,
                        DateUtil.getYMDDateString(dateRange.start),
                        DateUtil.getYMDDateString(dateRange.endInclusive)
                    )
            }

            val mostViewedPages = mostViewedArticlesDeferred.await().items
                .flatMap { it.rankItems }
                .filter { it.rank < 10 }
                .groupBy { it.wikiId to it.pageId }
                .map { (wikiAndPageId, items) ->
                    PageViews.PageItem(
                        wikiId = wikiAndPageId.first,
                        pageId = wikiAndPageId.second,
                        viewCount = items.sumOf { it.viewCount }
                    )
                }
                .sortedByDescending { it.viewCount }
                .let { pages ->
                    val topWikiId = pages.groupingBy { it.wikiId }
                        .fold(0L) { totalViews, page -> totalViews + page.viewCount }
                        .maxByOrNull { it.value }?.key
                    pages.filter { it.wikiId == topWikiId }
                }.take(10)

            val mostViewedPagesToViews = mutableListOf<Pair<PageTitle, Long>>()
            if (mostViewedPages.isNotEmpty()) {
                val wikiSite = WikiSite.forLanguageCode(mostViewedPages.first().wikiId.replace("wiki", ""))
                val pages = ServiceFactory.get(wikiSite)
                    .getInfoByPageIdsOrTitles(pageIds = mostViewedPages.map { it.pageId }.joinToString("|"))
                    .query?.pages.orEmpty().filter { it.ns == 0 && !mainPageTitles.contains(StringUtil.addUnderscores(it.title)) }

                mostViewedPages.forEach { page ->
                    pages.find { it.pageId.toLong() == page.pageId }?.let { pageInfo ->
                        mostViewedPagesToViews.add(
                            PageTitle(
                                text = pageInfo.title,
                                wiki = wikiSite,
                                thumbUrl = pageInfo.thumbUrl(),
                                description = pageInfo.description,
                                displayText = pageInfo.displayTitle(WikipediaApp.instance.appOrSystemLanguageCode)
                            ) to page.viewCount
                        )
                    }
                }
            }

            YearInReviewEditingStats(
                totalEditCount = totalEditsDeferred.await().items.sumOf { it.editCount },
                totalPageViewsCount = totalPageViewsDeferred.await().items.sumOf { it.viewCount },
                mostViewedArticles = mostViewedPagesToViews
            )
        } catch (e: IOException) {
            L.e(e)
            null
        }
    }
}
