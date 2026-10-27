package org.wikipedia.settings.dev.playground

import kotlinx.serialization.Serializable
import org.wikipedia.BuildConfig
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.yearinreview.data.YearInReviewAvailability
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewEditingStats
import org.wikipedia.yearinreview.data.YearInReviewEntryStats
import org.wikipedia.yearinreview.data.YearInReviewReadingStats
import org.wikipedia.yearinreview.data.YearInReviewRewardData
import org.wikipedia.yearinreview.data.YearInReviewSnapshot
import org.wikipedia.yearinreview.data.YearInReviewTimeOfDay
import org.wikipedia.yearinreview.presentation.YearInReviewPage
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

// Previews set pages, which replace the flow decider's pages. They're built on each load, so their sample data can vary.
enum class YearInReviewPlaygroundData(
    val label: String,
    val description: String,
    val readingStats: YearInReviewReadingStats?,
    val editingStats: YearInReviewEditingStats?,
    val previewPages: (() -> List<YearInReviewPage>)? = null
) {
    REAL("Real data", "Turns off the playground data. The card, announcement and slides use your real reading history, edits and login.", null, null),
    COLLECTIVE("Collective", "Fixed test data for a light reader, just under the data-rich and personalized thresholds, so it shows the low-data card " +
            "and announcement, and the collective slides. It doesn't use any of your real data.",
        readingStats = YearInReviewReadingStats(
            articlesReadCount = YearInReviewReadingStats.MIN_ARTICLES_READ - 1,
            visitedDaysCount = YearInReviewReadingStats.MIN_VISITED_DAYS - 1,
            peakMonth = 9,
            peakMonthVisitedDays = YearInReviewReadingStats.MIN_VISITED_DAYS - 1
        ),
        editingStats = YearInReviewEditingStats()
    ),
    PERSONALIZED("Personalized", "Fixed test data for the data-rich card and announcement, and the personalized slides. It doesn't use any of your real data.",
        readingStats = YearInReviewReadingStats(
            articlesReadCount = 120,
            visitedDaysCount = 45,
            totalTimeSpentSec = 98765,
            peakMonth = 12,
            peakMonthVisitedDays = 20,
            articlesRevisited = YearInReviewPlaygroundPreview.sampleArticles.map { it to 5 },
            articlesSavedTotal = 10,
            articlesSaved = YearInReviewPlaygroundPreview.sampleArticles,
            topCategory = "Fungi of Europe",
            topCountryCode = "RU",
            topCountryName = "Russia",
            topCountryArticleCount = 123,
            favoriteTimeOfDay = YearInReviewTimeOfDay.MORNING,
            favoriteTimeOfDayPercentage = 60,
            longestStreakDays = 40,
            longestStreakStartMonth = 5,
            longestStreakStartDay = 16,
            longestStreakEndMonth = 6,
            longestStreakEndDay = 30,
            biggestReadingDayMonth = 10,
            biggestReadingDayOfMonth = 25,
            biggestReadingDayTimeSpentSec = 123456,
            biggestReadingDayArticles = YearInReviewPlaygroundPreview.sampleArticles,
            longestReadArticles = YearInReviewPlaygroundPreview.sampleArticles,
            longestReadArticleMinutes = 123,
            rabbitHoleStart = YearInReviewPlaygroundPreview.sampleArticles.first(),
            rabbitHoleEnd = YearInReviewPlaygroundPreview.sampleArticles.last(),
            rabbitHoleFakeStart = YearInReviewPlaygroundPreview.sampleArticles.first(),
            rabbitHoleFakeEnd = YearInReviewPlaygroundPreview.sampleArticles.last(),
            rabbitHoleArticleCount = YearInReviewPlaygroundPreview.sampleArticles.size,
            topTopic = YearInReviewPlaygroundPreview.sampleTopTopic,
            topTopicTotalArticles = 123,
            topTopicSampleArticles = YearInReviewPlaygroundPreview.sampleTopTopicArticles,
            topicArticleCountMap = ArticleTopics.all.take(3).associate { it.topicId to (5..10).random() }
        ),
        editingStats = YearInReviewEditingStats(
            totalEditCount = 10,
            totalPageViewsCount = 1234567,
            mostViewedArticles = YearInReviewPlaygroundPreview.sampleArticles.map { it to 100000 }
        )
    ),
    ALL_SLIDES("All slides", "Every personalized slide, with the cover and closing slides.", null, null, { YearInReviewPlaygroundPreview.allSlides }),
    ALL_EMPTY_STATES("All empty states", "Every empty state slide, with the cover and closing slides.", null, null, { YearInReviewPlaygroundPreview.emptyStates })
}

val testActiveStartDate: LocalDate = LocalDate.of(YearInReviewConfig.YEAR, 12, 1)
val testActiveEndDate: LocalDate = LocalDate.of(YearInReviewConfig.YEAR + 1, 2, 1)

enum class YearInReviewPlaygroundDate(val label: String, val description: String, val date: LocalDate, val hiddenReason: String?) {
    BEFORE_START("Before start", "${testActiveStartDate.minusDays(1)}, the day before the active window opens.",
        testActiveStartDate.minusDays(1), "the date is before the active window"),
    ACTIVE("Active", "${testActiveStartDate.plusDays(14)}, inside the active window ($testActiveStartDate to $testActiveEndDate).",
        testActiveStartDate.plusDays(14), null),
    AFTER_END("After end", "$testActiveEndDate, the day the active window closes.",
        testActiveEndDate, "the date is after the active window")
}

// Test inputs for the entry point checks, which combine: each one can hide the entry point on its own.
@Serializable
data class YearInReviewPlaygroundEntryPoint(
    val useTestValues: Boolean = false,
    val hasRemoteConfig: Boolean = true,
    val date: YearInReviewPlaygroundDate = YearInReviewPlaygroundDate.ACTIVE,
    val countryCode: String = "RU"
) {
    fun canShowEntryPoint(isEnabled: Boolean, hiddenCountryCodes: List<String>): Boolean {
        return YearInReviewAvailability(
            YearInReviewConfig.YEAR,
            Clock.fixed(date.date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
        ).canShowEntryPoint(
            remoteConfig = if (hasRemoteConfig) testRemoteConfig(hiddenCountryCodes) else null,
            countryCode = countryCode,
            isEnabled = isEnabled
        )
    }

    // Every input that hides the entry point, so testers can see how their choices combine.
    // Without a remote config, the date and country aren't checked at all.
    fun hiddenReasons(isEnabled: Boolean, hiddenCountryCodes: List<String>): List<String> {
        return buildList {
            if (!isEnabled) {
                add("Year in Review is turned off in Settings")
            }
            if (!DeviceUtil.isOpenGlEs3Supported) {
                add("this device doesn't support OpenGL ES 3.0")
            }
            if (!hasRemoteConfig) {
                add("the remote config isn't published")
                return@buildList
            }
            date.hiddenReason?.let { add(it) }
            if (countryCode in hiddenCountryCodes) {
                add("$countryCode is a hidden country")
            }
        }
    }

    private fun testRemoteConfig(hiddenCountryCodes: List<String>): RemoteConfig.RemoteConfigImpl {
        return RemoteConfig.RemoteConfigImpl(
            androidv1 = RemoteConfig.RemoteConfigAndroidV1(
                yirMinAppVersion = BuildConfig.VERSION_CODE
            ),
            commonv1 = RemoteConfig.RemoteConfigCommonV1(
                yir = listOf(
                    RemoteConfig.RemoteConfigYearInReview(
                        year = YearInReviewConfig.YEAR,
                        activeStartDate = testActiveStartDate.atStartOfDay(),
                        activeEndDate = testActiveEndDate.atStartOfDay(),
                        hideCountryCodes = hiddenCountryCodes
                    )
                )
            )
        )
    }
}

object YearInReviewPlayground {
    // The one preview slide to show, or null for the whole deck.
    var previewPageId: String? = null

    // null when real data is chosen, and always outside pre-production builds
    val snapshot: YearInReviewSnapshot? get() {
        if (!ReleaseUtil.isPreProdRelease) {
            return null
        }
        val data = Prefs.yearInReviewPlaygroundData
        if (data == YearInReviewPlaygroundData.REAL) {
            return null
        }
        val remoteConfig = YearInReviewConfig.cachedRemoteConfig ?: YearInReviewConfig.fallbackConfig
        // Login stays real, so the personalized flow still needs a logged-in user.
        return YearInReviewSnapshot(
            year = YearInReviewConfig.YEAR,
            isLoggedIn = AccountUtil.isLoggedIn,
            isDonationEligible = true,
            remoteConfig = remoteConfig,
            readingStats = data.readingStats ?: YearInReviewReadingStats(articlesReadCount = 0, visitedDaysCount = 0, peakMonth = 0, peakMonthVisitedDays = 0),
            editingStats = data.editingStats ?: YearInReviewEditingStats(),
            rewardData = YearInReviewRewardData(isDonor = false, isEditor = false)
        )
    }

    // null unless fixed data is chosen, and always outside pre-production builds
    val entryStats: YearInReviewEntryStats? get() {
        if (!ReleaseUtil.isPreProdRelease) {
            return null
        }
        val readingStats = Prefs.yearInReviewPlaygroundData.readingStats ?: return null
        return YearInReviewEntryStats(articlesReadCount = readingStats.articlesReadCount, visitedDaysCount = readingStats.visitedDaysCount)
    }

    // null unless a preview is chosen, and always outside pre-production builds
    val pages: List<YearInReviewPage>? get() {
        if (!ReleaseUtil.isPreProdRelease) {
            return null
        }
        val previewPages = Prefs.yearInReviewPlaygroundData.previewPages?.invoke() ?: return null
        // The chosen slide, or the whole deck when none is chosen
        return previewPages.filter { it.id == previewPageId }.ifEmpty { previewPages }
    }

    // The countries hidden in the latest live remote config entry, since this year's entry may not be published yet
    val hiddenCountryCodes: List<String> get() {
        return RemoteConfig.config.commonv1?.yir?.maxByOrNull { it.year }?.hideCountryCodes?.distinct()?.ifEmpty { null } ?: listOf("RU")
    }

    // null when the real checks are chosen, and always outside pre-production builds.
    // The Year in Review setting stays real, so turning it off still hides the entry point.
    val canShowEntryPoint: Boolean? get() {
        val entryPoint = Prefs.yearInReviewPlaygroundEntryPoint
        if (!ReleaseUtil.isPreProdRelease || !entryPoint.useTestValues) {
            return null
        }
        return entryPoint.canShowEntryPoint(isEnabled = Prefs.isYearInReviewEnabled, hiddenCountryCodes = hiddenCountryCodes)
    }
}
