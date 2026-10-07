package org.wikipedia.settings.dev.playground

import kotlinx.serialization.Serializable
import org.wikipedia.auth.AccountUtil
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.yearinreview.data.YearInReviewAvailability
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewReadingStats
import org.wikipedia.yearinreview.data.YearInReviewRewardData
import org.wikipedia.yearinreview.data.YearInReviewSnapshot
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

enum class YearInReviewPlaygroundData(val label: String, val description: String, val readingStats: YearInReviewReadingStats?) {
    REAL("Real data", "Uses your real data, like reading history and edits. Add test entries below to try slide states.", null),
    LOW_DATA("Low data", "Fixed test data for the collective flow. Only tests the flow, it doesn't use any of your real data.",
        YearInReviewReadingStats(articlesReadCount = 0, visitedDaysCount = 0, peakMonth = 0, peakMonthVisitedDays = 0)),
    DATA_RICH("Data rich", "Fixed test data for the personalized flow. Only tests the flow, it doesn't use any of your real data.",
        YearInReviewReadingStats(articlesReadCount = 120, visitedDaysCount = 45, peakMonth = 12, peakMonthVisitedDays = 20))
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
        return YearInReviewAvailability(Clock.fixed(date.date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)).canShowEntryPoint(
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

    private fun testRemoteConfig(hiddenCountryCodes: List<String>): RemoteConfig.RemoteConfigYearInReview {
        return JsonUtil.json.decodeFromString(
            """
            {
              "year": ${YearInReviewConfig.YEAR},
              "activeStartDate": "${testActiveStartDate}T00:00:00",
              "activeEndDate": "${testActiveEndDate}T00:00:00",
              "hideCountryCodes": [${hiddenCountryCodes.joinToString { "\"$it\"" }}]
            }
            """
        )
    }
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
