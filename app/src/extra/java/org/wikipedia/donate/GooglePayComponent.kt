package org.wikipedia.donate

import android.app.Activity
import android.content.Intent
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.wikipedia.dataclient.donate.DonationConfigHelper
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal object GooglePayComponent {

    const val PAYMENTS_API_URL = "https://payments.wikimedia.org/"
    const val PAYMENT_METHOD_NAME = "paywithgoogle"
    const val CURRENCY_FALLBACK = "USD"
    const val TRANSACTION_FEE_PERCENTAGE = 0.04f

    private val CURRENCIES_THREE_DECIMAL = arrayOf("BHD", "CLF", "IQD", "KWD", "LYD", "MGA", "MRO", "OMR", "TND")
    private val CURRENCIES_NO_DECIMAL = arrayOf("CLP", "DJF", "IDR", "JPY", "KMF", "KRW", "MGA", "PYG", "VND", "XAF", "XOF", "XPF")

    private const val MERCHANT_NAME = "Wikimedia Foundation"
    private const val GATEWAY_NAME = "adyen"

    private const val GPAY_API_VERSION = 2
    private const val GPAY_API_VERSION_MINOR = 0

    private val allAllowedCardNetworks: List<String> = listOf("VISA", "MASTERCARD", "AMEX", "DISCOVER", "JCB", "INTERAC")
    private val allAllowedAuthMethods: List<String> = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")

    val baseCardPaymentMethod = JSONObject().apply {
        put("type", "CARD")
        put("parameters", JSONObject().apply {
            put("allowedCardNetworks", JSONArray(allAllowedCardNetworks))
            put("allowedAuthMethods", JSONArray(allAllowedAuthMethods))
        })
    }

    private val googlePayBaseConfiguration = JSONObject().apply {
        put("apiVersion", GPAY_API_VERSION)
        put("apiVersionMinor", GPAY_API_VERSION_MINOR)
        put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod))
    }

    fun getDecimalFormat(currencyCode: String, canonical: Boolean = false): DecimalFormat {
        val formatSpec = if (CURRENCIES_THREE_DECIMAL.contains(currencyCode)) "0.000" else if (CURRENCIES_NO_DECIMAL.contains(currencyCode)) "0" else "0.00"
        return if (canonical) DecimalFormat(formatSpec, DecimalFormatSymbols.getInstance(Locale.ROOT)) else DecimalFormat(formatSpec)
    }

    fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(if (Prefs.isDonationTestEnvironment) WalletConstants.ENVIRONMENT_TEST else WalletConstants.ENVIRONMENT_PRODUCTION).build()
        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    suspend fun isGooglePayAvailable(activity: Activity): Boolean {
        var available: Boolean
        withContext(Dispatchers.IO) {
            val readyToPayRequest = IsReadyToPayRequest.fromJson(googlePayBaseConfiguration.toString())
            val paymentsClient = createPaymentsClient(activity)
            val readyToPayTask = paymentsClient.isReadyToPay(readyToPayRequest)
            available = readyToPayTask.await()
            if (available) {
                DonationConfigHelper.getConfig()?.let { config ->
                    available = config.countryCodeGooglePayEnabled.contains(GeoUtil.geoIPCountry.orEmpty())
                }
            }
        }
        return available
    }

    fun getDonateActivityIntent(activity: Activity, campaignId: String? = null, donateUrl: String? = null): Intent {
        return GooglePayActivity.newIntent(activity, campaignId, donateUrl)
    }

    fun getPaymentDataRequestJson(
        amount: Float,
        currencyCode: String,
        merchantId: String?,
        gatewayMerchantId: String?
    ): JSONObject {
        val merchantInfo = JSONObject().apply {
            put("merchantName", MERCHANT_NAME)
            put("merchantId", merchantId)
        }

        val transactionInfo = JSONObject().apply {
            put("totalPrice", amount.toString())
            put("totalPriceStatus", "FINAL")
            put("currencyCode", currencyCode)
        }

        val tokenizationSpecification = JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put(
                "parameters", JSONObject(
                    mapOf(
                        "gateway" to GATEWAY_NAME,
                        "gatewayMerchantId" to gatewayMerchantId
                    )
                )
            )
        }

        val cardPaymentMethod = JSONObject().apply {
            put("type", "CARD")
            put("tokenizationSpecification", tokenizationSpecification)
            put("parameters", JSONObject().apply {
                put("allowedCardNetworks", JSONArray(allAllowedCardNetworks))
                put("allowedAuthMethods", JSONArray(allAllowedAuthMethods))
                put("billingAddressRequired", true)
                put("billingAddressParameters", JSONObject(mapOf("format" to "FULL")))
            })
        }

        val paymentDataRequestJson = JSONObject(googlePayBaseConfiguration.toString()).apply {
            put("apiVersion", GPAY_API_VERSION)
            put("apiVersionMinor", GPAY_API_VERSION_MINOR)
            put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))
            put("transactionInfo", transactionInfo)
            put("merchantInfo", merchantInfo)
            put("emailRequired", true)
        }

        return paymentDataRequestJson
    }
}
