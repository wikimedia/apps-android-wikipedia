package org.wikipedia.donate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.donate.DonationConfig
import org.wikipedia.dataclient.donate.DonationConfigHelper
import org.wikipedia.dataclient.donate.PaymentMethod
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L
import java.text.NumberFormat
import java.util.Locale

class GooglePayViewModel : ViewModel() {
    val uiState = MutableStateFlow(Resource<DonationConfig>())
    var donationConfig: DonationConfig? = null
    var paymentMethod: PaymentMethod? = null

    val currentCountryCode: String get() = GeoUtil.geoIPCountry.orEmpty()
    val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    val currencyCode get() = currencyFormat.currency?.currencyCode ?: "USD"
    val currencySymbol get() = currencyFormat.currency?.symbol ?: "$"
    val decimalFormat = GooglePayComponent.getDecimalFormat(currencyCode)

    val transactionFee: Float get() = donationConfig?.currencyTransactionFees?.get(currencyCode)
        ?: donationConfig?.currencyTransactionFees?.get("default") ?: 0f

    var finalAmount = 0f

    init {
        currencyFormat.minimumFractionDigits = 0
        load()
    }

    fun load() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            uiState.value = Resource.Error(throwable)
        }) {
            uiState.value = Resource.Loading()

            val paymentMethodsCall = async { ServiceFactory.get(WikiSite(GooglePayComponent.PAYMENTS_API_URL))
                .getPaymentMethods(currentCountryCode) }

            val donationConfigCall = async { DonationConfigHelper.getConfig() }

            donationConfig = donationConfigCall.await()
            val paymentMethods = paymentMethodsCall.await().response!!.paymentMethods
            paymentMethod = paymentMethods.find { it.type == GooglePayComponent.PAYMENT_METHOD_NAME }!!

            uiState.value = Resource.Success(donationConfig!!)
        }
    }

    fun getPaymentDataRequest(): PaymentDataRequest {
        return PaymentDataRequest.fromJson(GooglePayComponent.getPaymentDataRequestJson(finalAmount,
            currencyCode,
            paymentMethod?.configuration?.merchantId,
            paymentMethod?.configuration?.gatewayMerchantId
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

            val paymentDataObj = JSONObject(paymentData.toJson())
            val email = paymentDataObj.optString("email", "")
            val paymentMethodObj = paymentDataObj.getJSONObject("paymentMethodData")
            val infoObj = paymentMethodObj.getJSONObject("info")
            val billingObj = infoObj.getJSONObject("billingAddress")
            val token = paymentMethodObj.getJSONObject("tokenizationData").getString("token")

            val amount = decimalFormat.format(finalAmount)
            val locality = billingObj.optString("locality", "")
            val countryCode = infoObj.optString("countryCode", currentCountryCode)
            val fullName = billingObj.optString("name", "")
            val cardNetwork = infoObj.optString("cardNetwork", "")
            val postalCode = billingObj.optString("postalCode", "")
            val administrativeArea = billingObj.optString("administrativeArea", "")
            val address1 = billingObj.optString("address1", "")

            val response = ServiceFactory.get(WikiSite(GooglePayComponent.PAYMENTS_API_URL))
                .submitPayment(
                    amount,
                    BuildConfig.VERSION_NAME,
                    "", // TODO?
                    locality,
                    countryCode,
                    currencyCode,
                    currentCountryCode,
                    email,
                    "",
                    fullName,
                    WikipediaApp.instance.appOrSystemLanguageCode,
                    "",
                    if (recurring) "1" else "0",
                    token,
                    if (optInEmail) "1" else "0",
                    if (payTheFee) "1" else "0",
                    GooglePayComponent.PAYMENT_METHOD_NAME,
                    cardNetwork,
                    postalCode,
                    administrativeArea,
                    address1,
                )

            L.d("Payment response: $response")

            uiState.value = DonateSuccess()
        }
    }

    class DonateSuccess : Resource<DonationConfig>()
}
