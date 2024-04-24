package org.wikipedia.donate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.donate.DonationConfig
import org.wikipedia.dataclient.donate.DonationConfigHelper
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.Resource
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

class GooglePayViewModel : ViewModel() {
    val uiState = MutableStateFlow(Resource<DonationConfig>())
    var donationConfig: DonationConfig? = null

    private val currentCountryCode: String get() = GeoUtil.geoIPCountry.orEmpty()
    val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
    val decimalFormat = DecimalFormat("0.00")

    val currencyCode get() = currencyFormat.currency?.currencyCode ?: "USD"
    val currencySymbol get() = currencyFormat.currency?.symbol ?: "$"

    val transactionFee: Float get() = donationConfig?.currencyTransactionFees?.get(currencyCode)
        ?: donationConfig?.currencyTransactionFees?.get("default") ?: 0f

    var finalAmount = 0f

    init {
        // TODO: is this right?
        currencyFormat.minimumFractionDigits = 0
        currencyFormat.maximumFractionDigits = 2

        load()
    }

    fun load() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            uiState.value = Resource.Error(throwable)
        }) {
            uiState.value = Resource.Loading()

            // Careful: this call is rate-limited.
            // val paymentMethodsCall = async { ServiceFactory.get(WikiSite(GooglePayComponent.PAYMENTS_API_URL))
            //    .getPaymentMethods(currentCountryCode) }

            val donationConfigCall = async { DonationConfigHelper.getConfig() }

            donationConfig = donationConfigCall.await()
            // val paymentMethods = paymentMethodsCall.await().response!!.paymentMethods
            // paymentMethod = paymentMethods.find { it.type == "paywithgoogle" }!!

            uiState.value = Resource.Success(donationConfig!!)
        }
    }

    fun getPaymentDataRequest(): PaymentDataRequest {
        return PaymentDataRequest.fromJson(GooglePayComponent.getPaymentDataRequestJson(finalAmount,
            currencyCode,
            "TODO",
           "TODO"
        ).toString())
    }

    fun submit(
        paymentData: PaymentData,
        payTheFee: Boolean,
        recurring: Boolean,
        optInEmail: Boolean
    ) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            uiState.value = Resource.Error(throwable)
        }) {
            uiState.value = Resource.Loading()

            // TODO

            uiState.value = DonateSuccess()
        }
    }

    class DonateSuccess : Resource<DonationConfig>()
}
