package org.wikipedia.yearinreview.data

import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.donate.DonationResult
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class YearInReviewDonationEligibility(
    private val donationResultsProvider: () -> List<DonationResult> = { Prefs.donationResults }
) {
    fun hasDonatedWithinContributionsDateRange(
        remoteConfig: RemoteConfig.RemoteConfigYearInReview?
    ): Boolean {
        val dateRange = YearInReviewConfig.contributionsDateRange(remoteConfig)
        return donationResultsProvider().any { donationResult ->
            try {
                LocalDateTime.parse(donationResult.dateTime).toLocalDate() in dateRange
            } catch (_: DateTimeParseException) {
                false
            }
        }
    }

    suspend fun getDonorStatusFromWeb(): Boolean {
        val userInfo = ServiceFactory.get(WikiSite.forLanguageCode("en")).getUserInfo()
        val donationStatus = userInfo.query?.userInfo?.options?.wikimediaDonorStatus
        return !donationStatus.isNullOrEmpty() && donationStatus.toInt() >= 2
    }
}
