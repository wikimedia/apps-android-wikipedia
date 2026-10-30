package org.wikipedia.yearinreview.data

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.donate.DonationResult
import org.wikipedia.json.JsonUtil

class YearInReviewDonationEligibilityTest {

    private fun donorPref(
        value: Int,
        source: String? = "reader-donor-account",
        timestamp: Long = 1767225600000
    ): String = buildJsonObject {
        source?.let { put("source", it) }
        put("value", value)
        put("timestamp", timestamp)
    }.toString()

    private fun donationStatusFromWeb(donorStatus: String?): MwQueryResponse {
        val options = donorStatus?.let { """"options": {"wikimedia-donor": ${JsonPrimitive(it)}}""" } ?: """"options": {}"""
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
