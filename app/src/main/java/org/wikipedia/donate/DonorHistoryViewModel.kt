package org.wikipedia.donate

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wikipedia.Constants
import org.wikipedia.dataclient.restbase.RbDefinition
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource

class DonorHistoryViewModel(bundle: Bundle) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    var completedDonation = bundle.getBoolean(Constants.ARG_BOOLEAN)
    var isDonor = completedDonation || (Prefs.hasDonorHistorySaved && Prefs.donationResults.isNotEmpty())
    var lastDonated = Prefs.donationResults.lastOrNull()?.dateTime
    var isRecurringDonor = Prefs.isRecurringDonor

    private val _uiState = MutableStateFlow(Resource<List<RbDefinition.Usage>>())
    val uiState = _uiState.asStateFlow()

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DonorHistoryViewModel(bundle) as T
        }
    }
}
