package org.wikipedia.yearinreview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YearInReviewRewardDataTest {

    @Test
    fun `custom icon is locked without a donation or edit`() {
        assertFalse(YearInReviewRewardData(isDonor = false, isEditor = false).isCustomIconUnlocked)
    }

    @Test
    fun `custom icon is unlocked for a donor`() {
        assertTrue(YearInReviewRewardData(isDonor = true, isEditor = false).isCustomIconUnlocked)
    }

    @Test
    fun `custom icon is unlocked for an editor`() {
        assertTrue(YearInReviewRewardData(isDonor = false, isEditor = true).isCustomIconUnlocked)
    }
}
