package org.wikipedia.donate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import org.wikipedia.Constants
import org.wikipedia.settings.Prefs
import java.time.LocalDateTime
import java.time.ZoneId

class DonorHistoryViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    var completedDonation = savedStateHandle.get<Boolean>(Constants.ARG_BOOLEAN) == true
    var currentDonorStatus = -1
    var isDonor = completedDonation || (Prefs.hasDonorHistorySaved && Prefs.donationResults.isNotEmpty())
    var lastDonated = Prefs.donationResults.lastOrNull()?.dateTime
    var isRecurringDonor = Prefs.isRecurringDonor

    fun saveDonorHistory() {
        Prefs.hasDonorHistorySaved = true
        if (isDonor) {
            Prefs.isRecurringDonor = isRecurringDonor
            lastDonated?.let {
                // TODO: discuss that should we distinguish the donation from the same date.
                Prefs.donationResults = Prefs.donationResults.plus(DonationResult(it, false))
            }
        } else {
            Prefs.isRecurringDonor = false
            Prefs.donationResults = emptyList()
        }
    }

    fun dateTimeToMilli(dateTime: String): Long {
        return LocalDateTime.parse(dateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
