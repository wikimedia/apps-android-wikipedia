package org.wikipedia.donate

import android.app.Activity
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat

internal object GooglePayComponent {

    const val PAYMENTS_API_URL = "https://payments.wikimedia.org/"
    const val PAYMENT_METHOD_NAME = "paywithgoogle"

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

    fun getDecimalFormat(currencyCode: String): DecimalFormat {
        return DecimalFormat(if (CURRENCIES_THREE_DECIMAL.contains(currencyCode)) "0.000" else if (CURRENCIES_NO_DECIMAL.contains(currencyCode)) "0" else "0.00")
    }

    fun createPaymentsClient(activity: Activity): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            // .setEnvironment(WalletConstants.ENVIRONMENT_TEST).build()
            .setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION).build()
        return Wallet.getPaymentsClient(activity, walletOptions)
    }

    suspend fun isGooglePayAvailable(activity: Activity): Boolean {
        val readyToPayRequest = IsReadyToPayRequest.fromJson(googlePayBaseConfiguration.toString())
        val paymentsClient = createPaymentsClient(activity)
        val readyToPayTask = paymentsClient.isReadyToPay(readyToPayRequest)
        readyToPayTask.await()
        return readyToPayTask.result
    }

    fun onGooglePayButtonClicked(activity: Activity) {
        activity.startActivity(GooglePayActivity.newIntent(activity))
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
