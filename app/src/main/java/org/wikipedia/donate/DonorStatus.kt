package org.wikipedia.donate

import org.wikipedia.settings.Prefs

enum class DonorStatus {
    DONOR, NON_DONOR, UNKNOWN;

    companion object {
        fun donorStatus(): DonorStatus {
            // when donor status is set from developer settings
            Prefs.donorStatus?.let { return it}

            return if (Prefs.hasDonorHistorySaved.not()) {
                UNKNOWN
            } else if (Prefs.donationResults.isEmpty()) {
                NON_DONOR
            } else {
                DONOR
            }
        }
    }
}
