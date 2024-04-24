package org.wikipedia.dataclient.donate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError

@Suppress("unused")
@Serializable
class PaymentResponseContainer(
    val response: PaymentResponse? = null
)

@Suppress("unused")
@Serializable
class PaymentResponse(
    val status: String = "",
    @SerialName("error_message") val errorMessage: String = "",
    @SerialName("order_id") val orderId: String = "",
    @SerialName("gateway_transaction_id") val gatewayTransactionId: String = "",
    val paymentMethods: List<PaymentMethod> = emptyList()
) {
    init {
        if (status == "error") {
            throw MwException(MwServiceError("donate_error", errorMessage))
        }
    }
}

@Suppress("unused")
@Serializable
class PaymentMethod(
    val name: String = "",
    val type: String = "",
    val brands: List<String> = emptyList(),
    val configuration: PaymentMethodConfiguration? = null
)

@Suppress("unused")
@Serializable
class PaymentMethodConfiguration(
    val merchantId: String = "",
    val merchantName: String = "",
    val gatewayMerchantId: String = "",
    val storeId: String = "",
    val region: String = "",
    val publicKeyId: String = ""
)
