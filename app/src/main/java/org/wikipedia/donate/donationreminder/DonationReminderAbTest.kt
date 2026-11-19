package org.wikipedia.donate.donationreminder

import org.wikipedia.analytics.ABTest

class DonationReminderAbTest : ABTest("donationReminderV4", GROUP_SIZE_3) {

    override fun getGroupName(): String {
        return when (group) {
            GROUP_3 -> "f" // Variant C
            GROUP_2 -> "e" // Variant B
            else -> "d" // control
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2 || group == GROUP_3
    }
}
