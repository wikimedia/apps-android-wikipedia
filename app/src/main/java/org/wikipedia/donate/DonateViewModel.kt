package org.wikipedia.donate

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.donate.DonationConfigHelper
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.Resource
import java.util.Locale

class DonateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val uiState = _uiState.asStateFlow()

    fun checkGooglePayAvailable(activity: Activity) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = Resource.Error(throwable)
        }) {
            _uiState.value = Resource.Loading()

            GooglePayViewModel.updatePaymentMethodsPreferences()
            val donationConfig = async { DonationConfigHelper.getConfig() }
            var googlePayAvailable = GooglePayComponent.isGooglePayAvailable(activity)
            donationConfig.await()?.let { config ->
                val currentCountryCode = GeoUtil.geoIPCountry.orEmpty()
                val currencyCode = GeoUtil.currencyFormat(Locale.getDefault()).currency?.currencyCode ?: GooglePayComponent.CURRENCY_FALLBACK
                googlePayAvailable = !(Prefs.paymentMethodsMerchantId.isEmpty() ||
                        Prefs.paymentMethodsGatewayId.isEmpty() ||
                        !config.countryCodeGooglePayEnabled.contains(currentCountryCode) ||
                        !config.currencyAmountPresets.containsKey(currencyCode))
            }

            _uiState.value = Resource.Success(googlePayAvailable)
        }
    }
}
