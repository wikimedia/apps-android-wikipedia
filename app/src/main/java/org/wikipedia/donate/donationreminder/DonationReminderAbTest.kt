package org.wikipedia.donate.donationreminder

import org.wikipedia.analytics.ABTest

class DonationReminderAbTest : ABTest("donationReminderV4", GROUP_SIZE_3) {

    override fun getGroupName(): String {
        return when (group) {
            GROUP_3 -> "f" // Variant F
            GROUP_2 -> "e" // Variant E
            else -> "d" // Variant D (control)
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2 || group == GROUP_3
    }
}
