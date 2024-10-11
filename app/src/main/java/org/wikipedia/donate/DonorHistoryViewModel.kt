package org.wikipedia.donate

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.wikipedia.Constants
import org.wikipedia.settings.Prefs

class DonorHistoryViewModel(bundle: Bundle) : ViewModel() {

    var completedDonation = bundle.getBoolean(Constants.ARG_BOOLEAN)
    var isDonor = completedDonation || (Prefs.hasDonorHistorySaved && Prefs.donationResults.isNotEmpty())
    var lastDonated = Prefs.donationResults.lastOrNull()?.dateTime
    var isRecurringDonor = Prefs.isRecurringDonor

    fun saveDonorHistory() {
        Prefs.hasDonorHistorySaved = true
        Prefs.isRecurringDonor = isRecurringDonor
        lastDonated?.let {
            Prefs.donationResults = Prefs.donationResults.plus(DonationResult(it, false))
        }
    }

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DonorHistoryViewModel(bundle) as T
        }
    }
}
