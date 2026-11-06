package org.wikipedia.yearinreview.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
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

    private fun donationStatusFromWeb(donorPref: String?): MwQueryResponse {
        val options = donorPref?.let { """"options": {"wikimedia-donor": ${JsonPrimitive(it)}}""" } ?: """"options": {}"""
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

    @Test
    fun `contactable value 5 is not a donor`() = runBlocking {
        val response = donationStatusFromWeb(donorPref = donorPref(5))
        assertFalse(response.query?.userInfo?.options?.wikimediaDonorCode?.isRecentDonor == true)
    }

    @Test
    fun `malformed web pref is not a donor`() = runBlocking {
        val response = donationStatusFromWeb(donorPref = "{not json")
        assertFalse(response.query?.userInfo?.options?.wikimediaDonorCode?.isRecentDonor == true)
    }

    @Test
    fun `web values map to YiR donor status`() = runBlocking {
        mapOf(0 to false, 1 to true, 2 to true, 3 to false, 4 to true, 5 to false).forEach { (value, expected) ->
            val response = donationStatusFromWeb(donorPref = donorPref(value))
            assertEquals("value $value", expected, response.query?.userInfo?.options?.wikimediaDonorCode?.isRecentDonor == true)
        }
    }
}
