package org.wikipedia.yearinreview

import org.wikipedia.settings.RemoteConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

object YearInReviewConfig {
    const val YEAR = 2026
    private val fallbackContributionsDateRange = YearInReviewDateRange(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 11, 30))
    private val fallbackInsightsDateRange = YearInReviewDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 11, 30))

    val cachedRemoteConfig get() = RemoteConfig.config.commonv1?.getYirForYear(YEAR)

    // Used for the contributor slide and the donor/editor rewards/achievements
    fun contributionsDateRange(remoteConfig: RemoteConfig.RemoteConfigYearInReview?): YearInReviewDateRange {
        return dateRangeOrFallback(remoteConfig?.contributionsStartDate, remoteConfig?.contributionsEndDate, fallbackContributionsDateRange)
    }

    // Used for all other personalized insights
    fun insightsDateRange(remoteConfig: RemoteConfig.RemoteConfigYearInReview?): YearInReviewDateRange {
        return dateRangeOrFallback(remoteConfig?.dataStartDate, remoteConfig?.dataEndDate, fallbackInsightsDateRange)
    }

    // Both remote dates are needed, otherwise a half-configured year would mix remote and fallback dates.
    private fun dateRangeOrFallback(start: LocalDateTime?, endInclusive: LocalDateTime?, fallback: YearInReviewDateRange): YearInReviewDateRange {
        return if (start != null && endInclusive != null) {
            YearInReviewDateRange(start.toLocalDate(), endInclusive.toLocalDate())
        } else {
            fallback
        }
    }
}

data class YearInReviewDateRange(
    val start: LocalDate,
    val endInclusive: LocalDate
) {
    val startMillis get() = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    val endMillis get() = endInclusive.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC).toEpochMilli()

    operator fun contains(date: LocalDate) = date in start..endInclusive
}
