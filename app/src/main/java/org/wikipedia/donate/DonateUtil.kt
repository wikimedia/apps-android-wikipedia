package org.wikipedia.donate

import org.wikipedia.util.GeoUtil
import java.text.NumberFormat
import java.util.Locale

object DonateUtil {
    val currentCountryCode get() = GeoUtil.geoIPCountry.orEmpty()
    val currencyFormat: NumberFormat
        get() = NumberFormat.getCurrencyInstance(Locale.Builder()
            .setLocale(Locale.getDefault()).setRegion(currentCountryCode).build()).apply {
            minimumFractionDigits = 0
        }

    val currencyCode get() = currencyFormat.currency?.currencyCode ?: GooglePayComponent.CURRENCY_FALLBACK
    val currencySymbol get() = currencyFormat.currency?.symbol ?: "$"

    fun getAmountFloat(text: String): Float {
        var result: Float?
        result = text.toFloatOrNull()
        if (result == null) {
            val text2 = if (text.contains(".")) text.replace(".", ",") else text.replace(",", ".")
            result = text2.toFloatOrNull()
        }
        return result ?: 0f
    }
}
