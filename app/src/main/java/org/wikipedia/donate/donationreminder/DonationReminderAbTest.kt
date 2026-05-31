package org.wikipedia.donate.donationreminder

import org.wikipedia.analytics.ABTest

class DonationReminderAbTest : ABTest("donationReminder", GROUP_SIZE_2) {

    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "b" // test group
            else -> "a" // control
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }
}
