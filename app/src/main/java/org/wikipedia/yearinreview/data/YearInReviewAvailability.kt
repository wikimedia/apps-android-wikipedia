package org.wikipedia.yearinreview.data

import org.wikipedia.settings.RemoteConfig
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class YearInReviewAvailability(
    private val clock: Clock = Clock.systemUTC()
) {
    fun isAvailable(
        remoteConfig: RemoteConfig.RemoteConfigYearInReview?,
        countryCode: String?,
        developerOverride: Boolean = false
    ): Boolean {
        if (developerOverride) {
            return true
        }
        if (remoteConfig == null || countryCode in remoteConfig.hideCountryCodes) {
            return false
        }
        return isWithinActivePeriod(
            now = clock.instant(),
            start = remoteConfig.activeStartDate.toInstant(ZoneOffset.UTC),
            end = remoteConfig.activeEndDate.toInstant(ZoneOffset.UTC)
        )
    }

    fun canShowEntryPoint(
        remoteConfig: RemoteConfig.RemoteConfigYearInReview?,
        countryCode: String?,
        isEnabled: Boolean,
        developerOverride: Boolean = false
    ): Boolean {
        return isEnabled && isAvailable(remoteConfig, countryCode, developerOverride)
    }

    internal fun isWithinActivePeriod(now: Instant, start: Instant, end: Instant): Boolean {
        return !now.isBefore(start) && now.isBefore(end)
    }
}
