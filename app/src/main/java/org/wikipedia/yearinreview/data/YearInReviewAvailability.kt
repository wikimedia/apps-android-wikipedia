package org.wikipedia.yearinreview.data

import org.wikipedia.BuildConfig
import org.wikipedia.settings.RemoteConfig
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class YearInReviewAvailability(
    private val year: Int,
    private val clock: Clock = Clock.systemUTC()
) {
    fun isAvailable(
        remoteConfig: RemoteConfig.RemoteConfigImpl?,
        countryCode: String?,
        developerOverride: Boolean = false
    ): Boolean {
        if (developerOverride) {
            return true
        }
        val config = remoteConfig?.commonv1?.getYirForYear(year)
        if (config == null || countryCode in config.hideCountryCodes || (remoteConfig.androidv1?.yirMinAppVersion ?: Int.MAX_VALUE) > BuildConfig.VERSION_CODE) {
            return false
        }
        return isWithinActivePeriod(
            now = clock.instant(),
            start = config.activeStartDate.toInstant(ZoneOffset.UTC),
            end = config.activeEndDate.toInstant(ZoneOffset.UTC)
        )
    }

    fun canShowEntryPoint(
        remoteConfig: RemoteConfig.RemoteConfigImpl?,
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
