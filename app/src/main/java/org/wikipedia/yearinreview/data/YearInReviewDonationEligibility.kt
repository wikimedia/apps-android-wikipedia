package org.wikipedia.yearinreview.data

import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.donate.DonationResult
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.log.L
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class YearInReviewDonationEligibility(
    private val donationResultsProvider: () -> List<DonationResult> = { Prefs.donationResults },
    private val donationStatusFromWebProvider: suspend () -> MwQueryResponse = {
        ServiceFactory.get(WikiSite.forLanguageCode("en")).getUserInfo()
    }
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
        return AccountUtil.isLoggedIn && try {
            val donationStatus = donationStatusFromWebProvider().query?.userInfo?.options?.wikimediaDonorStatus
            donationStatus in DONOR_CODES
        } catch (e: Exception) {
            L.e(e)
            false
        }
    }

    suspend fun isDonor(remoteConfig: RemoteConfig.RemoteConfigYearInReview?): Boolean {
        return getDonorStatusFromWeb() || hasDonatedWithinContributionsDateRange(remoteConfig)
    }

    companion object {
        // TODO: Update set with appropriate codes
        private val DONOR_CODES = setOf(2, 4, 6, 12, 14, 16, 20, 25, 30, 35)
    }
}
