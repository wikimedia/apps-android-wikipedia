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
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class GooglePayViewModel : ViewModel() {
    val uiState = MutableStateFlow(Resource<DonationConfig>())
    private var donationConfig: DonationConfig? = null
    private val currentCountryCode get() = GeoUtil.geoIPCountry.orEmpty()

    val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.Builder()
        .setLocale(Locale.getDefault()).setRegion(currentCountryCode).build())
    val currencyCode get() = currencyFormat.currency?.currencyCode ?: GooglePayComponent.CURRENCY_FALLBACK
    val currencySymbol get() = currencyFormat.currency?.symbol ?: "$"
    val decimalFormat = GooglePayComponent.getDecimalFormat(currencyCode)

    val transactionFee get() = donationConfig?.currencyTransactionFees?.get(currencyCode)
        ?: donationConfig?.currencyTransactionFees?.get("default") ?: 0f

    val minimumAmount get() = donationConfig?.currencyMinimumDonation?.get(currencyCode) ?: 0f

    val maximumAmount: Float get() {
        var max = donationConfig?.currencyMaximumDonation?.get(currencyCode) ?: 0f
        if (max == 0f) {
            val defaultMin = donationConfig?.currencyMinimumDonation?.get(GooglePayComponent.CURRENCY_FALLBACK) ?: 0f
            if (defaultMin > 0f) {
                max = (donationConfig?.currencyMinimumDonation?.get(currencyCode) ?: 0f) / defaultMin *
                        (donationConfig?.currencyMaximumDonation?.get(GooglePayComponent.CURRENCY_FALLBACK) ?: 0f)
            }
        }
        return max
    }

    val emailOptInRequired get() = donationConfig?.countryCodeEmailOptInRequired.orEmpty().contains(currentCountryCode)

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

            val donationConfigCall = async { DonationConfigHelper.getConfig() }

            donationConfig = donationConfigCall.await()

            // The paymentMethods API is rate limited, so we cache it manually.
            val now = Instant.now().epochSecond
            if (abs(now - Prefs.paymentMethodsLastQueryTime) > TimeUnit.DAYS.toSeconds(7)) {
                Prefs.paymentMethodsMerchantId = ""
                Prefs.paymentMethodsGatewayId = ""

                val paymentMethodsCall = async {
                    ServiceFactory.get(WikiSite(GooglePayComponent.PAYMENTS_API_URL))
                        .getPaymentMethods(currentCountryCode)
                }
                paymentMethodsCall.await().response?.let { response ->
                    Prefs.paymentMethodsLastQueryTime = now
                    response.paymentMethods.find { it.type == GooglePayComponent.PAYMENT_METHOD_NAME }?.let {
                        Prefs.paymentMethodsMerchantId = it.configuration?.merchantId.orEmpty()
                        Prefs.paymentMethodsGatewayId = it.configuration?.gatewayMerchantId.orEmpty()
                    }
                }
            }

            if (Prefs.paymentMethodsMerchantId.isEmpty() ||
                Prefs.paymentMethodsGatewayId.isEmpty() ||
                !donationConfig!!.countryCodeGooglePayEnabled.contains(currentCountryCode) ||
                !donationConfig!!.currencyAmountPresets.containsKey(currencyCode)) {
                uiState.value = NoPaymentMethod()
            } else {
                uiState.value = Resource.Success(donationConfig!!)
            }
        }
    }

    fun getPaymentDataRequest(): PaymentDataRequest {
        return PaymentDataRequest.fromJson(GooglePayComponent.getPaymentDataRequestJson(finalAmount,
            currencyCode,
            Prefs.paymentMethodsMerchantId,
            Prefs.paymentMethodsGatewayId
        ).toString())
    }

    fun submit(
        paymentData: PaymentData,
        payTheFee: Boolean,
        recurring: Boolean,
        optInEmail: Boolean,
        campaignId: String
    ) {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            uiState.value = Resource.Error(throwable)
        }) {
            uiState.value = Resource.Loading()

            if (Prefs.isDonationTestEnvironment) {
                uiState.value = DonateSuccess()
                return@launch
            }

            val paymentDataObj = JSONObject(paymentData.toJson())
            val paymentMethodObj = paymentDataObj.getJSONObject("paymentMethodData")
            val infoObj = paymentMethodObj.getJSONObject("info")
            val billingObj = infoObj.getJSONObject("billingAddress")
            val token = paymentMethodObj.getJSONObject("tokenizationData").getString("token")

            // The backend expects the final amount in the canonical decimal format, instead of
            // any localized format, e.g. comma as decimal separator.
            val decimalFormatCanonical = GooglePayComponent.getDecimalFormat(currencyCode, true)

            val response = ServiceFactory.get(WikiSite(GooglePayComponent.PAYMENTS_API_URL))
                .submitPayment(
                    decimalFormatCanonical.format(finalAmount),
                    BuildConfig.VERSION_NAME,
                    campaignId,
                    billingObj.optString("locality", ""),
                    currentCountryCode,
                    currencyCode,
                    billingObj.optString("countryCode", currentCountryCode),
                    paymentDataObj.optString("email", ""),
                    billingObj.optString("name", ""),
                    WikipediaApp.instance.appOrSystemLanguageCode,
                    if (recurring) "1" else "0",
                    token,
                    if (optInEmail) "1" else "0",
                    if (payTheFee) "1" else "0",
                    GooglePayComponent.PAYMENT_METHOD_NAME,
                    infoObj.optString("cardNetwork", ""),
                    billingObj.optString("postalCode", ""),
                    billingObj.optString("administrativeArea", ""),
                    billingObj.optString("address1", ""),
                )

            L.d("Payment response: $response")

            uiState.value = DonateSuccess()
        }
    }

    class NoPaymentMethod : Resource<DonationConfig>()
    class DonateSuccess : Resource<DonationConfig>()
}
