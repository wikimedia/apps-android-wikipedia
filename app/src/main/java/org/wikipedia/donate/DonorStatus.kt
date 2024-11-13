package org.wikipedia.donate

import org.wikipedia.settings.Prefs

enum class DonorStatus {
    DONOR, NON_DONOR, UNKNOWN;

    companion object {

        val isDonor get() = donorStatus() == DONOR

        fun donorStatus(): DonorStatus {
            return if (Prefs.hasDonorHistorySaved.not()) {
                UNKNOWN
            } else if (Prefs.isDonor) {
                DONOR
            } else {
                NON_DONOR
            }
        }
    }
}
