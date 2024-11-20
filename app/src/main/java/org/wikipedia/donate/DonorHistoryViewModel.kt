package org.wikipedia.donate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import org.wikipedia.Constants
import org.wikipedia.settings.Prefs
import org.wikipedia.usercontrib.ContributionsDashboardHelper
import java.time.LocalDateTime
import java.time.ZoneId

class DonorHistoryViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    val completedDonation = savedStateHandle.get<Boolean>(Constants.ARG_BOOLEAN) == true
    val shouldGoBackToContributeTab = savedStateHandle.get<Boolean>(DonorHistoryActivity.RESULT_GO_BACK_TO_CONTRIBUTE_TAB) == true
    var currentDonorStatus = if (completedDonation) 0 else -1
    var isDonor = completedDonation || Prefs.hasDonorHistorySaved && Prefs.isDonor
    var lastDonated = Prefs.donationResults.lastOrNull()?.dateTime
    var isRecurringDonor = Prefs.isRecurringDonor
    var donorHistoryModified = false

    fun saveDonorHistory() {
        Prefs.hasDonorHistorySaved = true
        ContributionsDashboardHelper.showSurveyDialogUI = true
        if (isDonor) {
            Prefs.isRecurringDonor = isRecurringDonor
            lastDonated?.let {
                Prefs.donationResults = Prefs.donationResults.plus(DonationResult(it, false)).distinct()
            }
        } else {
            Prefs.isRecurringDonor = false
            Prefs.donationResults = emptyList()
        }
        Prefs.isDonor = isDonor
        donorHistoryModified = false
    }

    fun dateTimeToMilli(dateTime: String): Long {
        return LocalDateTime.parse(dateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
