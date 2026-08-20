package org.wikipedia.donate.donationreminder

import org.wikipedia.analytics.ABTest

class DonationReminderAbTest : ABTest("donationReminder", GROUP_SIZE_3) {

    override fun getGroupName(): String {
        return when (group) {
            GROUP_3 -> "c" // Variant C
            GROUP_2 -> "b" // Variant B
            else -> "a" // control
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2 || group == GROUP_3
    }
}
