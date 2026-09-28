package org.wikipedia.yearinreview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikipedia.donate.DonationResult

class YearInReviewDonationEligibilityTest {
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
