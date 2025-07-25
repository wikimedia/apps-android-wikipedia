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

    // Users with a device location in Germany, France, Poland, or Philippines
    private val enabledCountries = listOf(
        "DE", "FR", "PL", "PH"
    )

    // Users with a primary language of German, English, French, or Polish
    private val enabledLanguages = listOf(
        "de", "fr", "pl", "en"
    )

    // TODO: update the end date when before release to production for 30-day experiment
    val isEnabled get() = ReleaseUtil.isDevRelease ||
            (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                    enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                    LocalDate.now() <= LocalDate.of(2025, 12, 1) && !AccountUtil.isLoggedIn)

    // @TODO: update PH data after PM confirmation
    val currencyAmountPresets = mapOf(
        "FR" to listOf(1, 2, 3),
        "DE" to listOf(1, 2, 3),
        "PL" to listOf(1, 2, 3),
        "PH" to listOf(100, 200, 300)
    )
}
