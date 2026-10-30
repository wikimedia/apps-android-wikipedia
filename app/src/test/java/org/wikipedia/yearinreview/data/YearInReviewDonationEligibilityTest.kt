package org.wikipedia.yearinreview.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.donate.DonationResult
import org.wikipedia.json.JsonUtil

class YearInReviewDonationEligibilityTest {

    private fun donationStatusFromWeb(donorStatus: String?): MwQueryResponse {
        val options = donorStatus?.let { """"options": {"wikimedia-donor": $it}""" } ?: """"options": {}"""
        return JsonUtil.json.decodeFromString(
            """{"batchcomplete": true, "query": {"userinfo": {"id": 1, "name": "Test", $options}}}"""
        )
    }

    @Test
    fun `donation inside contribution period is eligible`() {
        val eligibility = YearInReviewDonationEligibility {
            listOf(DonationResult(dateTime = "2026-06-15T12:00:00"))
        }

        assertTrue(eligibility.hasDonatedWithinContributionsDateRange(remoteConfig = null))
    }

    @Test
    fun `donation outside contribution period is not eligible`() {
        val eligibility = YearInReviewDonationEligibility {
            listOf(DonationResult(dateTime = "2025-11-30T23:59:59"))
        }

        assertFalse(eligibility.hasDonatedWithinContributionsDateRange(remoteConfig = null))
    }

    @Test
    fun `invalid donation date is not eligible`() {
        val eligibility = YearInReviewDonationEligibility {
            listOf(DonationResult(dateTime = "not-a-date"))
        }

        assertFalse(eligibility.hasDonatedWithinContributionsDateRange(remoteConfig = null))
    }
}
