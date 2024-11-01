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
    var isDonor = completedDonation || (Prefs.hasDonorHistorySaved && (Prefs.donationResults.isNotEmpty() || Prefs.isRecurringDonor))
    var lastDonated = Prefs.donationResults.lastOrNull()?.dateTime
    var isRecurringDonor = Prefs.isRecurringDonor
    var donorHistoryModified = false

    fun saveDonorHistory() {
        Prefs.hasDonorHistorySaved = true
        if (isDonor) {
            Prefs.isRecurringDonor = isRecurringDonor
            lastDonated?.let {
                Prefs.donationResults = Prefs.donationResults.plus(DonationResult(it, false)).distinct()
            }
        } else {
            Prefs.isRecurringDonor = false
            Prefs.donationResults = emptyList()
        }
        donorHistoryModified = false
    }

    fun dateTimeToMilli(dateTime: String): Long {
        return LocalDateTime.parse(dateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
