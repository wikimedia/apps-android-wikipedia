package org.wikipedia.yearinreview.data

import org.wikipedia.settings.RemoteConfig
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

object YearInReviewConfig {
    const val YEAR = 2026

    val fallbackConfig = RemoteConfig.RemoteConfigYearInReview(
        year = YEAR,
        activeStartDate = LocalDateTime.of(2025, 12, 1, 0, 0),
        activeEndDate = LocalDateTime.of(2026, 11, 30, 23, 59),
        dataStartDate = LocalDateTime.of(2026, 1, 1, 0, 0),
        dataEndDate = LocalDateTime.of(2026, 11, 30, 23, 59),
        contributionsStartDate = LocalDateTime.of(2025, 12, 1, 0, 0),
        contributionsEndDate = LocalDateTime.of(2026, 11, 30, 23, 59),
        languages = 300,
        articles = 65667790,
        savedArticlesApps = 46392587,
        viewsApps = 14085467928,
        editsApps = 1936242,
        editsPerMinute = 324,
        averageArticlesReadPerYear = 335,
        edits = 78952894,
        editsEN = 31004338,
        bytesAddedEN = 3471067588,
        hoursReadEN = 2376881343,
        yearsReadEN = 270000
    )

    val cachedRemoteConfig get() = RemoteConfig.config.commonv1?.getYirForYear(YEAR)

    // Used for the contributor slide and the donor/editor rewards/achievements
    fun contributionsDateRange(remoteConfig: RemoteConfig.RemoteConfigYearInReview?): YearInReviewDateRange {
        return dateRangeOrFallback(remoteConfig?.contributionsStartDate, remoteConfig?.contributionsEndDate,
            YearInReviewDateRange(fallbackConfig.contributionsStartDate!!.toLocalDate(), fallbackConfig.contributionsEndDate!!.toLocalDate()))
    }

    // Used for all other personalized insights
    fun insightsDateRange(remoteConfig: RemoteConfig.RemoteConfigYearInReview?): YearInReviewDateRange {
        return dateRangeOrFallback(remoteConfig?.dataStartDate, remoteConfig?.dataEndDate,
            YearInReviewDateRange(fallbackConfig.dataStartDate!!.toLocalDate(), fallbackConfig.dataEndDate!!.toLocalDate()))
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
