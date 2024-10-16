package org.wikipedia.donate

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.wikipedia.Constants
import org.wikipedia.settings.Prefs
import java.time.LocalDateTime
import java.time.ZoneId

class DonorHistoryViewModel(bundle: Bundle) : ViewModel() {

    var completedDonation = bundle.getBoolean(Constants.ARG_BOOLEAN)
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

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DonorHistoryViewModel(bundle) as T
        }
    }
}
