package org.wikipedia.donate

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.util.Resource

class DonateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val uiState = _uiState.asStateFlow()

    fun checkGooglePayAvailable(activity: Activity) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = Resource.Error(throwable)
        }) {
            _uiState.value = Resource.Loading()

            _uiState.value = Resource.Success(GooglePayComponent.isGooglePayAvailable(activity))
        }
    }
}
