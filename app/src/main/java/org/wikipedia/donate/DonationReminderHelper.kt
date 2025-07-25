package org.wikipedia.donate

import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

object DonationReminderHelper {

    val currentCountryCode get() = GeoUtil.geoIPCountry.orEmpty()
    val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.Builder()
        .setLocale(Locale.getDefault()).setRegion(currentCountryCode).build())

    private val enabledCountries = listOf(
        "IT"
    )

    private val enabledLanguages = listOf(
        "it", "en"
    )

    // TODO: update the end date when before release to production for 30-day experiment
    val isEnabled get() = ReleaseUtil.isDevRelease ||
            (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                    enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                    LocalDate.now() <= LocalDate.of(2025, 12, 1) && !AccountUtil.isLoggedIn)

    val currencyAmountPresets = mapOf(
        "IT" to listOf(1, 2, 3)
    )
}
